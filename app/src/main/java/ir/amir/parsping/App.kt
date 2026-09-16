package ir.amir.parsping

import android.app.Application
import com.adivery.sdk.Adivery

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Adivery.configure(this, AdiveryIds.APP_ID)

        // Warm up both placements as early as possible so they're ready
        // by the time the user opens the app / taps connect.
        Adivery.prepareAppOpenAd(this, AdiveryIds.APP_OPEN_PLACEMENT_ID)
        Adivery.prepareRewardedAd(this, AdiveryIds.REWARDED_PLACEMENT_ID)
    }
}
