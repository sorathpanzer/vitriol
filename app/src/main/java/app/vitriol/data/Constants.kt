package app.vitriol.data

internal object Constants {
    object Dialog {
        const val ABOUT = "ABOUT"
    }

    object SwipeAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
        const val APP = 3
        const val LOCKSCREEN = 4
        const val NULL = 0
    }

    object TextSize {
        const val ONE = 0.6f
        const val TWO = 0.75f
        const val THREE = 0.9f
        const val FOUR = 1f
        const val FIVE = 1.15f
        const val SIX = 1.3f
        const val SEVEN = 1.45f
    }

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_SWIPE_LEFT_APP = 10
    const val FLAG_SET_SWIPE_RIGHT_APP = 11
    const val FLAG_SET_ONE_TAP_APP = 12
    const val FLAG_SET_DOUBLE_TAP_APP = 13
    const val FLAG_SET_SWIPE_UP_APP = 14
    const val FLAG_SET_SWIPE_DOWN_APP = 15
    const val FLAG_SET_TWOFINGER_SWIPE_UP_APP = 16
    const val FLAG_SET_TWOFINGER_SWIPE_DOWN_APP = 17
    const val FLAG_SET_TWOFINGER_SWIPE_LEFT_APP = 18
    const val FLAG_SET_TWOFINGER_SWIPE_RIGHT_APP = 19
    const val FLAG_SET_PINCH_IN_APP = 20
    const val FLAG_SET_PINCH_OUT_APP = 21

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 10f

    const val URL_ABOUT_VITRIOL = "https://github.com/sorathpanzer/vitriol"
    const val URL_DUCK_SEARCH = "https://duckduckgo.com?q="
}

internal object Navigation {
    const val HOME = "home"
    const val APP_DRAWER = "app_drawer"
    const val SETTINGS = "settings"
    const val HIDDEN_APPS = "hidden_apps"
}
