package zlcook.com.step;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * 设置界面
 */
public class SetActivity extends Activity implements NumberPicker.OnValueChangeListener{
    //-------存储设置数据---------
    //储存器
    public static SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    //------变量---------
    private int gender=1;//性别标识，1：男，0：女
    private int tizhong=60;//体重
    private int shengao=173;//身高
    private int tixing =0;//低速提醒
    //------控件---------
    //体重、身高、低速控件
    private NumberPicker np_tizhong,np_shengao,np_tixing;
    //性别女、男控件
    private ImageButton ib_nv,ib_nan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        //获取控件
        //体重控件、身高控件、性别控件、提醒控件
        np_tizhong = (NumberPicker) findViewById(R.id.np_tizhong);
        np_shengao = (NumberPicker) findViewById(R.id.np_shengao);
        np_tixing = (NumberPicker) findViewById(R.id.np_tixing);

        ib_nan = (ImageButton) findViewById(R.id.ib_nan);
        ib_nv = (ImageButton) findViewById(R.id.ib_nv);

        //设置体重控件、身高控件、提醒控件值变化监听器
        np_tizhong.setOnValueChangedListener(this);
        np_shengao.setOnValueChangedListener(this);
        np_tixing.setOnValueChangedListener(this);
        //设置体重控件、身高控件的最小值、最大值
        np_shengao.setMaxValue(250);
        np_shengao.setMinValue(80);
        np_tizhong.setMaxValue(150);
        np_tizhong.setMinValue(20);
        np_tixing.setMinValue(0);
        np_tixing.setMaxValue(10);

        //初始化存储器，及操作存储器的编辑器
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(SettingParams.JIBUQI_SHARED_PREFERENCES, MODE_PRIVATE);
        }
        editor = sharedPreferences.edit();

    }


    //完成设置，保存设置数据到存储器中
    public void wancheng(View view){

        //将所有值保存到存储器中，并更新全局参数SettingParams中的值
        editor.putInt(SettingParams.GENDER_NAME, gender);
        editor.putInt(SettingParams.TIZHONG_NAME, tizhong);
        editor.putInt(SettingParams.SHENGAO_NAME, shengao);
        editor.putInt(SettingParams.TIXING_NAME, tixing);
        editor.commit();
        SettingParams.GENDER=gender;
        SettingParams.TIZHONG=tizhong;
        SettingParams.SHENGAO=shengao;
        SettingParams.TIXING=tixing;
        //返回
        this.onBackPressed();
    }
    //返回按钮
    public void fanhui(View view){
        this.onBackPressed();
    }

    //选择性别女
    public void gender_nv(View view){
        gender = 0;
        refreshGenderView(gender);
    }
    //选择性别男
    public void gender_nan(View view){
        gender = 1;
        refreshGenderView(gender);
    }

    /**
     * 根据性别刷新性别控件
     * @param gender  1:男，0：女
     */
    public void refreshGenderView(int gender){
        if(gender==1){//男
            ib_nan.setBackgroundResource(R.drawable.nan);
            ib_nv.setBackgroundResource(R.drawable.huinv);
        }else { //选择性别女
            ib_nan.setBackgroundResource(R.drawable.huinan);
            ib_nv.setBackgroundResource(R.drawable.nv);
        }
    }

    //每次界面重新出现时，就更新一下数据
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        //初始化设置参数的值
        initParmsSetData();
        //刷新所有参数控件值
        reflushAllParamsViewValue();
    }
    //初始化设置参数的值
    public void initParmsSetData() {
        //使用全局设置参数初始化数据
        gender=SettingParams.GENDER;
        tizhong=SettingParams.TIZHONG;
        shengao=SettingParams.SHENGAO;
        tixing=SettingParams.TIXING;
    }
    //根据设置数据刷新所有参数控件值
    public void reflushAllParamsViewValue() {
        //体重、身高控件
        np_tizhong.setValue(tizhong);
        np_shengao.setValue(shengao);
        np_tixing.setValue(tixing);
        //性别控件
        refreshGenderView(gender);
    }

    //监听体重、身高控件值变化
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        //身高
        if( picker.getId() == R.id.np_shengao){
            shengao = newVal;
        }
        //体重
        if( picker.getId() == R.id.np_tizhong){
            tizhong = newVal;
        }
        //低速提醒
        if( picker.getId() == R.id.np_tixing){
            tixing = newVal;
        }
    }
}
