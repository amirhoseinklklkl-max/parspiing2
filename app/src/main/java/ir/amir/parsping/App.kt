package ir.amir.parsping

import android.app.Application
import com.adivery.sdk.Adivery

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Adivery.configure(this, AdiveryIds.APP_ID)

        // Warm up the rewarded placement as early as possible so it's ready
        // by the time the user taps connect. App-open ads need an Activity
        // context, so that one is prepared from MainActivity instead.
        Adivery.prepareRewardedAd(this, AdiveryIds.REWARDED_PLACEMENT_ID)
    }
}
