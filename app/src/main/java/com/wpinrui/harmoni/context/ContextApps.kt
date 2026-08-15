package com.wpinrui.harmoni.context

/**
 * Every app the contextual rules name, resolved against what is actually installed.
 *
 * The rules are written in terms of these rather than raw strings so a package rename is one
 * edit, and so a rule reads as the app it is about.
 */
object ContextApps {

    // Payments and banking
    const val PAYLAH = "com.dbs.dbspaylah"
    const val TRUST = "sg.trust"
    const val WALLET = "com.google.android.apps.walletnfcrel"
    const val SC_MOBILE = "air.app.scb.breeze.android.main.sg.prod"
    const val CITIBANK = "com.citibank.mobile.sg"
    const val DBS_DIGIBANK = "com.dbs.sg.dbsmbanking"
    const val OCBC = "com.ocbc.mobile"

    // Getting about
    const val MAPS = "com.google.android.apps.maps"
    const val NTU_OMNIBUS = "sg.edu.ntu.apps.ntuomnibus"
    const val SINGABUS = "sg.cotton.singabus"
    const val GRAB = "com.grabtaxi.passenger"

    // Talking to people
    const val OUTLOOK = "com.microsoft.office.outlook"
    const val CALENDAR = "com.google.android.calendar"
    const val WHATSAPP = "com.whatsapp"

    // The rest
    const val KCUTS_GO = "com.kcutsgo.customer"
    const val SETTINGS = "com.android.settings"

    /** Everything a rule can put on the ring, which is the candidate set before scoring. */
    val all = listOf(
        PAYLAH, TRUST, WALLET, SC_MOBILE, CITIBANK, DBS_DIGIBANK, OCBC,
        MAPS, NTU_OMNIBUS, SINGABUS, GRAB,
        OUTLOOK, CALENDAR, WHATSAPP,
        KCUTS_GO, SETTINGS,
    )
}
