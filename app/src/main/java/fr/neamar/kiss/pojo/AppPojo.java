package fr.neamar.kiss.pojo;

import android.os.UserManager;

import fr.neamar.kiss.utils.UserHandle;

public class AppPojo extends PojoWithTags {
    public final String packageName;
    public final String activityName;
    public final UserHandle userHandle;
    public final boolean isEnabled;

    public AppPojo(String id, String packageName, String activityName, UserHandle userHandle, boolean isEnabled) {
        super(id);

        this.packageName = packageName;
        this.activityName = activityName;
        this.userHandle = userHandle;
        this.isEnabled = isEnabled;
    }

    public String getComponentName() {
        return userHandle.addUserSuffixToString(packageName + "/" + activityName, '#');
    }
}
