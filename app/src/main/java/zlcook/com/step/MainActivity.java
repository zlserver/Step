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
import java.util.TooManyListenersException;

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

    //------控件------

    //定义文本框控件
    private TextView tv_show_step;// 步数
    private TextView tv_timer;// 运行时间
    private TextView tv_distance;// 行程
    private TextView tv_calories;// 卡路里
    private TextView tv_velocity;// 平均速度（总路程/总时间）
    private TextView tv_curr_velocity;// 瞬时速度（5秒钟路程/5s）

    //  跑步暂停/开始按钮
    private ImageButton ib_start_pause;
    //底部跑步控制按钮，开始跑步后显示出来
    private LinearLayout ll_pao_kong;
    //底部跑步按钮，跑步前显示出来
    private LinearLayout ll_pao_init;

    //定时器，开启跑步后，定时获取计步服务得到的数据
    private Timer timer=null;
    //循环周期，单位毫秒，每隔CYC_PERIOD毫秒刷新一次数据
    private int CYC_PERIOD=300;


    //-----统计步数相关变量-------
    private  long timing = 0;// 记录每次运动的实时消耗总时间，单位毫秒。
    private  long startTimer = 0;// 点击开始按钮后的时间点，单位毫秒。
    private  long tempTime = 0;  //记录运动的从第一次开始到最后一次暂停消耗的总时间，暂停再次开始后的时间不会加上来。timer=tempTime+瞬时时间-startTimer，单位毫秒

    private Double distance = 0.0;// 路程：米
    private Double calories = 0.0;// 热量：卡路里
    private Double velocity = 0.0;// 速度：米每秒
    private int total_step = 0;   //走的总步数

    // ----------求瞬时速速相关变量--------------

    private Double curr_velocity = 0.0;// 瞬时速度：米每秒  curr_velocity = curr_distance / CURR_VELOCITY_TIME_PERIOD ;（瞬时速度=瞬时距离/瞬时时间长度）
    private int CURR_VELOCITY_TIME_PERIOD=5000;//瞬时速度的时间长度，单位毫秒（5s=5000毫秒）
    /**
     * curr_distance（瞬时距离）为CURR_VELOCITY_TIME_PERIOD时间内走的距离，单位：米。
     * 因为数据是每隔CYC_PERIOD毫秒刷新一次。curr_cyc_period_distance（瞬时CYC_PERIOD毫秒走的距离）=distance（瞬时刷新后的总距离）- pre_distance（上一次刷新后的总距离）
     * (瞬时速度的时间长度产生的循环次数)curr_cyc_count= CURR_VELOCITY_TIME_PERIOD / CYC_PERIOD ;
     *（curr_distance = curr_distance + 新的curr_cyc_period_distance - curr_distance中最早的curr_cyc_period_distance）每次新一轮刷新完成后，就把最新一轮刷新产生的curr_cyc_period_distance距离加上curr_distance，然后在减去curr_distance中最前面一次循环的curr_cyc_period_distance距离。
     */
    private Double curr_distance=0.0;
    //瞬时速度的时间长度可以产生的刷新次数
    private int cyc_count_of_curr_velocity_time_period=CURR_VELOCITY_TIME_PERIOD/CYC_PERIOD;
    //用于存储最近cyc_count_of_curr_velocity_time_period次，产生的距离。  curr_distance = 该数组元素之和
    private Double[] cyc_period_distance_array=new Double[cyc_count_of_curr_velocity_time_period];

    //跑步开始后，多少秒后才开启低速提醒，因为一开始跑没有速度，所以为了防止一开始就发出低速提醒，所以设置这个时间
    private long LOW_SPEED_WARNING_START_TIME =30000;//单位毫秒

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
        tv_curr_velocity = (TextView) this.findViewById(R.id.curr_velocity);


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

    @Override
    public void onBackPressed() {
        //关闭跑步服务
        stopPaobuService();
        super.onBackPressed();
    }

    /**
     *  初始化控件和变量
     */
    public void initValue(){
        distance = 0.0;// 路程：米
        calories = 0.0;// 热量：卡路里
        velocity = 0.0;// 速度：米每秒
        curr_velocity=0.0;// 瞬时速度：米每秒
        total_step = 0;//总步数
        //初始化距离数组
        initDistanceArray(cyc_period_distance_array);
        StepDetector.CURRENT_SETP=0;//归零计步服务得到的步数

        timing = 0;// 记录每次运动的实时消耗总时间。
        startTimer = 0;// 点击开始按钮后的时间点。
        tempTime = 0; //记录运动的从第一次开始到最后一次暂停消耗的总时间，暂停再次开始后的时间不会加上来。timer=tempTime+当前时间-startTimer
        //为控件设置值
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
        //开启定时器,每隔CYC_PERIOD毫秒执行一次
        executeTimerTask(CYC_PERIOD);
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
     * @param period  每隔多长时间执行一次，单位毫秒
     */
    public void executeTimerTask(long period){
        //开启定时器,每隔period毫秒执行一次
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
        //存储刷新前的距离
        Double pre_distance = distance;

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
            
            //刷新后前进的距离
            Double curr_cyc_period_distance= distance - pre_distance;
            //瞬时前进距离加入到最近cyc_count_of_curr_velocity_time_period次前进距离数组中
            pushCurrDistanceArray(cyc_period_distance_array,curr_cyc_period_distance);
            //获取最近cyc_count_of_curr_velocity_time_period次产生的距离之和
            curr_distance = calcuCurrDistance(cyc_period_distance_array);
            //计算瞬时速度
            curr_velocity = (curr_distance * 1000) / CURR_VELOCITY_TIME_PERIOD; //因为CURR_VELOCITY_TIME_PERIOD是毫秒
            
        } else {
            calories = 0.0;
            velocity = 0.0;
            curr_velocity=0.0;
        }
        //调用步数方法
        if (total_step % 2 == 0) {
            total_step = StepDetector.CURRENT_SETP;
        } else {
            total_step = StepDetector.CURRENT_SETP +1;
        }

        //开始超过30秒后开启低速提醒
        if(curr_velocity<SettingParams.TIXING && timing >LOW_SPEED_WARNING_START_TIME){
            //播放提示声音
            if( soundPool==null)
            soundPool= new SoundPool(5,AudioManager.STREAM_SYSTEM, 5);
            soundPool.load(MainActivity.this,R.raw.warnsong,1);
            soundPool.play(1,1, 1, 0, 0, 1);

        }
    }
    /**
     * 瞬时前进距离加入到最近cyc_count_of_curr_velocity_time_period次前进距离数组中，数组长度固定，采用先进先出原则进行数据淘汰
     * @param cyc_period_distance_array  存放每次刷新前进的距离数组
     * @param curr_cyc_period_distance  刷新产生的前进距离
     */
    private void pushCurrDistanceArray(Double[] cyc_period_distance_array, Double curr_cyc_period_distance) {
        //淘汰最旧的数
        int i =0;
        for(  i =0 ;i < cyc_period_distance_array.length-1;i++){
            cyc_period_distance_array[i]=cyc_period_distance_array[i+1];
        }
        //最新的数插入到最后面
        cyc_period_distance_array[i]=curr_cyc_period_distance;
    }

    /**
     * 计算数组元素之和
     * @param cyc_period_distance_array
     * @return
     */
    private Double calcuCurrDistance(Double[] cyc_period_distance_array){
        Double total = 0.0;
        for(Double dis : cyc_period_distance_array)
            total+=dis;
        return total;
    }
    /**
     * 初始化距离数组
     * @param cyc_period_distance_array
     */
    public void initDistanceArray(Double[] cyc_period_distance_array){
        for( int i = 0;i < cyc_period_distance_array.length;i++)
            cyc_period_distance_array[i]=0.0;
    }
    /**
     设置控件值
     */
    public void setViewValue(){
        tv_show_step.setText(total_step + "");// 显示当前步数
        tv_distance.setText(formatDouble(distance)+ "m");// 显示路程
        tv_velocity.setText(formatDouble(velocity)+"m/s");// 显示速度
        tv_curr_velocity.setText(formatDouble(curr_velocity)+"m/s");// 显示瞬时速度
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
