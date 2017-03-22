package zlcook.com.step;

/**
 * Created by dell on 2017/2/18.
 * 保存参数的值全局类
 */
public class SettingParams {
    // 计步器设置数据在PREFERENCES中的标识
    public static final String JIBUQI_SHARED_PREFERENCES = "jibuqi_shared_preferences";
    //各参数在存储器中的名称
    public static final String GENDER_NAME ="shengao", TIZHONG_NAME ="tizhong", SHENGAO_NAME ="shengao", TIXING_NAME ="tixing";

    //保存各个参数的变量以及默认值
    public static int GENDER=1;//性别标识，1：男，0：女
    public static int TIZHONG=60;//体重kg
    public static int SHENGAO=173;//身高cm
    public static int TIXING =0;//单位：m/s 低速提醒，低于这个速度就算为低速。


    /**
     * 获取步长值
     * @return
     */
    public static int getSepLength(){
        // 步长和身高、性别相关
        // 男生公式：步长=身高*0.45
        //女生公式：步长=身高*0.45 * 0.9
        int step_len=0;
        if( GENDER==0)
            step_len=(int)(SHENGAO*0.45*0.9);
        else
            step_len=(int)(SHENGAO*0.45);
        return step_len;
    }

}
