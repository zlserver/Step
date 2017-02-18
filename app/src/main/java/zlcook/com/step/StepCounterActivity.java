package zlcook.com.step;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import com.ant.liao.GifView;

import java.text.DecimalFormat;

public class StepCounterActivity extends AppCompatActivity {


    //定义文本框控件
    private TextView tv_show_step;// 步数

    private TextView tv_timer;// 运行时间

    private TextView tv_distance;// 行程
    private TextView tv_calories;// 卡路里
    private TextView tv_velocity;// 速度

    private Button btn_start;// 开始按钮
    private Button btn_stop;// 停止按钮

    private GifView gifView;  //小鸭子动画图片

    private long timer = 0;// 记录每次运动的实时消耗总时间。
    private  long startTimer = 0;// 点击开始按钮后的时间点。

    private  long tempTime = 0;  //记录运动的从第一次开始到最后一次暂停消耗的总时间，暂停再次开始后的时间不会加上来。timer=tempTime+当前时间-startTimer

    private Double distance = 0.0;// 路程：米
    private Double calories = 0.0;// 热量：卡路里
    private Double velocity = 0.0;// 速度：米每秒

    private int step_length = 0;  //步长
    private int weight = 0;       //体重
    private int total_step = 0;   //走的总步数

    private Thread thread;  //定义线程对象

    private TableRow hide1, hide2;
    private TextView step_counter;

    // 当创建一个新的Handler实例时, 它会绑定到当前线程和消息的队列中,开始分发数据
    // Handler有两个作用
    // (1) : 定时执行Message和Runnalbe 对象
    // (2): 让一个动作,在不同的线程中执行.

    //每次计步服务开启后，通过该消息队列来更新界面的数据
    Handler handler = new Handler() {// Handler对象用于更新当前步数,定时发送消息，调用方法查询数据用于显示？？？？？？？？？？
        //主要接受子线程发送的数据, 并用此数据配合主线程更新UI
        //Handler运行在主线程中(UI线程中), 它与子线程可以通过Message对象来传递数据,
        //Handler就承担着接受子线程传过来的(子线程用sendMessage()方法传递Message对象，(里面包含数据)
        //把这些消息放入主线程队列中，配合主线程进行更新UI。

        @Override                  //这个方法是从父类/接口 继承过来的，需要重写一次
        public void handleMessage(Message msg) {
            super.handleMessage(msg);        // 此处可以更新UI
            countDistance();     //调用距离方法，看一下走了多远
            if (timer != 0 && distance != 0.0) {
                // 体重、距离
                // 跑步热量（kcal）＝体重（kg）×距离（公里）×1.036
                calories = weight * distance * 0.001;
                //速度velocity
                velocity = distance * 1000 / timer;
            } else {
                calories = 0.0;
                velocity = 0.0;
            }
            countStep();          //调用步数方法
            tv_show_step.setText(total_step + "");// 显示当前步数
            tv_distance.setText(formatDouble(distance));// 显示路程
            tv_calories.setText(formatDouble(calories));// 显示卡路里
            tv_velocity.setText(formatDouble(velocity));// 显示速度
            tv_timer.setText(getFormatTime(timer));// 显示当前运行时间

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.main);  //设置当前屏幕

        //获取上次保存的步长、体重信息
        if (SettingsActivity.sharedPreferences == null) {
            SettingsActivity.sharedPreferences = this.getSharedPreferences(
                    SettingsActivity.SETP_SHARED_PREFERENCES,
                    Context.MODE_PRIVATE);
        }
        //初始化小鸭子图片
        gifView = (GifView)findViewById(R.id.gif_view);
        gifView.setGifImageType(GifView.GifImageType.COVER);
        gifView.setShowDimension(100, 100);
        gifView.setGifImage(R.drawable.run_gif);
        gifView.showCover();

        //开启线程监听当前步数的变化
        if (thread == null) {
            thread = new Thread() {// 子线程用于监听当前步数的变化

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    super.run();
                    int temp = 0;
                    //循环检测数据变化，每个300毫秒更新一次，更新主界面的数值。
                    while (true) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //计步服务正在运行，则更新数据
                        if (StepCounterService.FLAG) {
                            Message msg = new Message();
                            if (temp != StepDetector.CURRENT_SETP) {//
                                temp = StepDetector.CURRENT_SETP;
                            }
                            if (startTimer != System.currentTimeMillis()) {
                                timer = tempTime + System.currentTimeMillis()
                                        - startTimer;
                            }
                            handler.sendMessage(msg);// 通知主线程
                        }
                    }
                }
            };
            thread.start();
        }
        // 获取界面控件
        addView();

        // 初始化控件
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("APP", "on resuame.");
    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    /**
     * 获取Activity相关控件，同时停止掉计步数据更新线程和计步服务
     */
    private void addView() {
        tv_show_step = (TextView) this.findViewById(R.id.show_step);

        tv_timer = (TextView) this.findViewById(R.id.timer);

        tv_distance = (TextView) this.findViewById(R.id.distance);
        tv_calories = (TextView) this.findViewById(R.id.calories);
        tv_velocity = (TextView) this.findViewById(R.id.velocity);

        btn_start = (Button) this.findViewById(R.id.start);
        btn_stop = (Button) this.findViewById(R.id.stop);

        hide1 = (TableRow)findViewById(R.id.hide1);
        hide2 = (TableRow)findViewById(R.id.hide2);
        step_counter = (TextView)findViewById(R.id.step_counter);

        //计步数据更新线程停止
      //  handler.removeCallbacks(thread);
        //停止计步服务
       // Intent service = new Intent(this, StepCounterService.class);
       // stopService(service);
        StepDetector.CURRENT_SETP = 0;
        tempTime = timer = 0;
        tv_timer.setText(getFormatTime(timer));      //如果关闭之后，格式化时间
        tv_show_step.setText("0");
        tv_distance.setText(formatDouble(0.0));
        tv_calories.setText(formatDouble(0.0));
        tv_velocity.setText(formatDouble(0.0));

    }


    /**
     * 初始化界面
     */
    private void init() {
        //步长、体重
        step_length = SettingsActivity.sharedPreferences.getInt(
                SettingsActivity.STEP_LENGTH_VALUE, 70);
        weight = SettingsActivity.sharedPreferences.getInt(
                SettingsActivity.WEIGHT_VALUE, 50);
        //计算距离和步数
        countDistance();
        countStep();
        if ((timer += tempTime) != 0 && distance != 0.0) {  //tempTime记录运动的总时间，timer记录每次运动时间
            // 体重、距离
            // 跑步热量（kcal）＝体重（kg）×距离（公里）×1.036，换算一下
            calories = weight * distance * 0.001;

            velocity = distance * 1000 / timer;
        } else {
            calories = 0.0;
            velocity = 0.0;
        }

        tv_timer.setText(getFormatTime(timer + tempTime));

        tv_distance.setText(formatDouble(distance));
        tv_calories.setText(formatDouble(calories));
        tv_velocity.setText(formatDouble(velocity));

        tv_show_step.setText(total_step + "");

        btn_start.setEnabled(!StepCounterService.FLAG);
        btn_stop.setEnabled(StepCounterService.FLAG);

        if (StepCounterService.FLAG) {
            btn_stop.setText(getString(R.string.pause));
        } else if (StepDetector.CURRENT_SETP > 0) {
            btn_stop.setEnabled(true);
            btn_stop.setText(getString(R.string.cancel));
        }

    }

    /**
     * 计算并格式化doubles数值，保留两位有效数字
     *
     * @param doubles
     * @return 返回当前路程
     */
    private String formatDouble(Double doubles) {
        DecimalFormat format = new DecimalFormat("####.##");
        String distanceStr = format.format(doubles);
        return distanceStr.equals(getString(R.string.zero)) ? getString(R.string.double_zero)
                : distanceStr;
    }

    public void onClick(View view) {
        Intent service = new Intent(this, StepCounterService.class);
        switch (view.getId()) {
            case R.id.start:              //开始计算步数
                gifView.showAnimation();  //小丫开始动
                startService(service);    //开启计步服务
                btn_start.setEnabled(false);  //开始按钮不可动
                btn_stop.setEnabled(true);
                btn_stop.setText(getString(R.string.pause));
                startTimer = System.currentTimeMillis(); //开始时间和结束时间一致
                tempTime = timer;
                StepDetector.CURRENT_SETP = total_step;
                break;
            case R.id.stop:
                stopService(service);
                gifView.showCover();
                if (StepCounterService.FLAG && StepDetector.CURRENT_SETP > 0) {
                    btn_stop.setText(getString(R.string.cancel));
                } else {
                    StepDetector.CURRENT_SETP = 0;
                    total_step=0;
                    tempTime = timer = 0;

                    btn_stop.setText(getString(R.string.pause));
                    btn_stop.setEnabled(false);

                    tv_timer.setText(getFormatTime(timer));      //如果关闭之后，格式化时间

                    tv_show_step.setText("0");
                    tv_distance.setText(formatDouble(0.0));
                    tv_calories.setText(formatDouble(0.0));
                    tv_velocity.setText(formatDouble(0.0));

                    handler.removeCallbacks(thread);
                }
                btn_start.setEnabled(true);
                break;
        }
    }

    /**
     * 得到一个格式化的时间
     *
     * @param time
     *            时间 毫秒
     * @return 时：分：秒：毫秒
     */
    private String getFormatTime(long time) {
        time = time / 1000;
        long second = time % 60;
        long minute = (time % 3600) / 60;
        long hour = time / 3600;

        // 毫秒秒显示两位
        // String strMillisecond = "" + (millisecond / 10);
        // 秒显示两位
        String strSecond = ("00" + second)
                .substring(("00" + second).length() - 2);
        // 分显示两位
        String strMinute = ("00" + minute)
                .substring(("00" + minute).length() - 2);
        // 时显示两位
        String strHour = ("00" + hour).substring(("00" + hour).length() - 2);

        return strHour + ":" + strMinute + ":" + strSecond;
        // + strMillisecond;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_step, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.ment_information:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 计算行走的距离
     */
    private void countDistance() {
        if (StepDetector.CURRENT_SETP % 2 == 0) {
            distance = (StepDetector.CURRENT_SETP / 2) * 3 * step_length * 0.01;
        } else {
            distance = ((StepDetector.CURRENT_SETP / 2) * 3 + 1) * step_length * 0.01;
        }
    }

    /**
     * 实际的步数
     */
    private void countStep() {
        if (StepDetector.CURRENT_SETP % 2 == 0) {
            total_step = StepDetector.CURRENT_SETP;
        } else {
            total_step = StepDetector.CURRENT_SETP +1;
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        finish();
    }
}
