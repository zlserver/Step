package zlcook.com.step;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 主界面
 */
public class MainActivity extends Activity {

    //-------储存器--------
    public static SharedPreferences sharedPreferences;
    //---------跑步状态--------
    private  final int PAO_OVER=0;//跑步结束（跑步未开始）
    private  final int PAO_PAUSE=1;//跑步暂停中
    private  final int PAO_ING=2;//跑步进行中
    //当前跑步状态
    private int pao_state_status=PAO_OVER;

    //-----统计步数相关变量-------
    private  long timing = 0;// 记录每次运动的实时消耗总时间。
    private  long startTimer = 0;// 点击开始按钮后的时间点。
    private  long tempTime = 0;  //记录运动的从第一次开始到最后一次暂停消耗的总时间，暂停再次开始后的时间不会加上来。timer=tempTime+当前时间-startTimer

    private Double distance = 0.0;// 路程：米
    private Double calories = 0.0;// 热量：卡路里
    private Double velocity = 0.0;// 速度：米每秒
    private int total_step = 0;   //走的总步数

    //------控件------

    //定义文本框控件
    private TextView tv_show_step;// 步数
    private TextView tv_timer;// 运行时间
    private TextView tv_distance;// 行程
    private TextView tv_calories;// 卡路里
    private TextView tv_velocity;// 速度

    //  跑步暂停/开始按钮
    private ImageButton ib_start_pause;
    //底部跑步控制按钮，开始跑步后显示出来
    private LinearLayout ll_pao_kong;
    //底部跑步按钮，跑步前显示出来
    private LinearLayout ll_pao_init;

    //定时器，开启跑步后，定时获取计步服务得到的数据
    private Timer timer=null;
    //播放提示声音使用
    private SoundPool soundPool;

    //消息通道，用于异步修改控件数据
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //计算各个指标的值
            calcuData();
            //刷新控件值
            setViewValue();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ll_pao_kong = (LinearLayout) findViewById(R.id.pao_kong);
        ll_pao_init = (LinearLayout) findViewById(R.id.pao_init);
        ib_start_pause = (ImageButton) findViewById(R.id.start_pause);
        tv_show_step = (TextView) this.findViewById(R.id.show_step);
        tv_timer = (TextView) this.findViewById(R.id.timer);
        tv_distance = (TextView) this.findViewById(R.id.distance);
        tv_calories = (TextView) this.findViewById(R.id.calories);
        tv_velocity = (TextView) this.findViewById(R.id.velocity);

        //初始化变量和控件值
        initValue();
        //页面第一次启动时，获取保存在存储器中的设置数据，将其提取到SettingParams类中，之后设置数据值只有在设置界面点击完成后才会发生改变
        getSetData();
    }


    //参数设置，跳转到设置界面
    public void canshushezhi(View view){
        Intent intent = new Intent(this, SetActivity.class);
        startActivity(intent);
    }

    //开始跑步
    public void kaishipao(View view){
        //底部界面改变
        pao_state_status=PAO_ING;
        setPaoBuStatusView();

        //初始化控件和变量
        initValue();
        //初始化跑步服务
        initPaobuService();
        //开始计时
        //此时点击开始后的时间点
        startTimer = System.currentTimeMillis(); //开始时间和结束时间一致
    }


    //结束跑步
    public void jieshupaobu(View view){
        //底部界面改变
        pao_state_status=PAO_OVER;
        setPaoBuStatusView();
        //关闭跑步服务
        stopPaobuService();

    }
    //暂停或继续跑步
    public void zantingOrkaishi(View view){
        if( pao_state_status==PAO_ING )//当前是跑步进行中
        {
            pao_state_status=PAO_PAUSE;//设置为跑步暂停
            //关闭跑步服务
            stopPaobuService();
            tempTime = timing;
            //保存当前总步数
            total_step=StepDetector.CURRENT_SETP;
        }else  if(pao_state_status==PAO_PAUSE){//当前是跑步暂停中
            pao_state_status=PAO_ING;//设置为跑步进行中

            //计步服务步数设为暂停之前的总步数
            StepDetector.CURRENT_SETP=total_step;
            startTimer = System.currentTimeMillis();
            //初始化跑步服务
            initPaobuService();
        }
        //设置底部按钮
        setPaoBuStatusView();
    }

    /**
     *  初始化控件和变量
     */
    public void initValue(){
        distance = 0.0;// 路程：米
        calories = 0.0;// 热量：卡路里
        velocity = 0.0;// 速度：米每秒
        total_step = 0;//总步数
        StepDetector.CURRENT_SETP=0;//归零计步服务得到的步数

        timing = 0;// 记录每次运动的实时消耗总时间。
        startTimer = 0;// 点击开始按钮后的时间点。
        tempTime = 0; //记录运动的从第一次开始到最后一次暂停消耗的总时间，暂停再次开始后的时间不会加上来。timer=tempTime+当前时间-startTimer

        setViewValue();
    }

    /**
     获取设置数据
     */
    public void getSetData() {
        // TODO Auto-generated method stub
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(SettingParams.JIBUQI_SHARED_PREFERENCES, MODE_PRIVATE);
        }
        //获取设置数据:性别、体重、身高、提醒
        SettingParams.GENDER = sharedPreferences.getInt(SettingParams.GENDER_NAME,SettingParams.GENDER);
        SettingParams.TIZHONG = sharedPreferences.getInt(SettingParams.TIZHONG_NAME,SettingParams.TIZHONG );
        SettingParams.SHENGAO = sharedPreferences.getInt(SettingParams.SHENGAO_NAME,SettingParams.SHENGAO);
        SettingParams.TIXING = sharedPreferences.getInt(SettingParams.TIXING_NAME,SettingParams.TIXING);
    }

    /**
     * 根据跑步状态设置底部按钮的显示
     * pao_state_status 跑步状态：0:未跑步，1:正在跑步中，2:暂停跑步中
     */
    public void setPaoBuStatusView(){

        switch (pao_state_status){
            case PAO_OVER:  //跑步结束（跑步未开始）
                //底部跑步控制按钮隐藏，跑步按钮显示
                ll_pao_kong.setVisibility(View.GONE);
                ll_pao_init.setVisibility(View.VISIBLE);
                Toast.makeText(this,"结束跑步",Toast.LENGTH_SHORT).show();
              break;
            case PAO_ING: //跑步进行中
                //设为暂停图标
                ib_start_pause.setBackgroundResource(R.drawable.zanting);
                //底部跑步控制按钮显示，跑步按钮隐藏
                ll_pao_kong.setVisibility(View.VISIBLE);
                ll_pao_init.setVisibility(View.GONE);
                Toast.makeText(this,"跑步中....",Toast.LENGTH_SHORT).show();
                break;
            case PAO_PAUSE:  //跑步暂停中
                //设为开始图标
                ib_start_pause.setBackgroundResource(R.drawable.kaishi);

                //底部跑步控制按钮显示，跑步按钮隐藏
                ll_pao_kong.setVisibility(View.VISIBLE);
                ll_pao_init.setVisibility(View.GONE);
                Toast.makeText(this,"暂停中....",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 初始化跑步服务
     */
    public void initPaobuService(){
        Intent service = new Intent(this, StepCounterService.class);
        //计步服务开始
        startService(service);
        //开启定时器,每隔300毫秒执行一次
        executeTimerTask(300);
    }

    /**
     * 关闭跑步服务
     */
    public void stopPaobuService(){
        //关闭定时器任务
        closeTimerTask();

        Intent service = new Intent(this, StepCounterService.class);
        //计步服务关闭
        stopService(service);
    }
    /**
     * 执行定时器任务
     * @param period  每隔多长时间执行一次
     */
    public void executeTimerTask(long period){
        //开启定时器,每隔300毫秒执行一次
        if( timer ==null)
            timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //更新步数
                total_step= StepDetector.CURRENT_SETP;

                //通知异步刷新控件
                handler.sendEmptyMessage(0);
            }
        },0,period);
    }
    /**
     * 关闭定时器任务
     */
    public void closeTimerTask(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }


    /**
     * 计算各个指标的值
     */
    public void calcuData(){
        //获得消耗总时间
        if (startTimer != System.currentTimeMillis()) {
            timing = tempTime + System.currentTimeMillis()
                    - startTimer;
        }
        //调用距离方法，看一下走了多远
        if (total_step % 2 == 0) {
            distance = (total_step / 2) * 3 * SettingParams.getSepLength() * 0.01;
        } else {
            distance = ((total_step / 2) * 3 + 1) * SettingParams.getSepLength() * 0.01;
        }
        if (timing != 0 && distance != 0.0) {
            // 体重、距离
            // 跑步热量（kcal）＝体重（kg）×距离（公里）×1.036
            calories =SettingParams.TIZHONG  * distance * 0.001*1.036;
            //速度velocity
            velocity = distance * 1000 / timing;
        } else {
            calories = 0.0;
            velocity = 0.0;
        }
        //调用步数方法
        if (total_step % 2 == 0) {
            total_step = StepDetector.CURRENT_SETP;
        } else {
            total_step = StepDetector.CURRENT_SETP +1;
        }

        //开始超过30秒后开启低速提醒，并且每个2秒给一个提醒
        if(velocity<SettingParams.TIXING && timing >30000){
            //播放提示声音
            if( soundPool==null)
            soundPool= new SoundPool(5,AudioManager.STREAM_SYSTEM, 5);
            soundPool.load(MainActivity.this,R.raw.warnsong,1);
            soundPool.play(1,1, 1, 0, 0, 1);

        }
    }
    /**
     设置控件值
     */
    public void setViewValue(){
        tv_show_step.setText(total_step + "");// 显示当前步数
        tv_distance.setText(formatDouble(distance)+ "m");// 显示路程
        tv_velocity.setText(formatDouble(velocity)+"m/s");// 显示速度
        tv_calories.setText(formatDouble(calories)+"k");// 显示卡路里
        tv_timer.setText(getFormatTime(timing));// 显示当前运行时间
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
}
