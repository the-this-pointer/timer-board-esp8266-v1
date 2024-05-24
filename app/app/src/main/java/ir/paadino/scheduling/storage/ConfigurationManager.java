package ir.paadino.scheduling.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by r.kiani on 05/22/2015.
 */

public class ConfigurationManager {

    private SharedPreferences sharedPreferences;
    private static ConfigurationManager mInstance = null;

    private String mScheduleData;

    public static ConfigurationManager getInstance(Context context) {
        if(mInstance == null) mInstance =  new ConfigurationManager(context);
        mInstance.updateInstance();
        return mInstance;
    }

    public ConfigurationManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void updateInstance()
    {
        this.mScheduleData = sharedPreferences.getString("prefSchedule", "");
    }

    public String getScheduleData() {
        return mScheduleData;
    }

    public void setScheduleData(String scheduleData) {
        sharedPreferences.edit().putString("prefSchedule", scheduleData).apply();
        this.mScheduleData = scheduleData;
    }
}
