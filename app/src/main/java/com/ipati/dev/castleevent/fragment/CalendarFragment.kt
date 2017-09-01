package com.ipati.dev.castleevent.fragment

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Paint
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.android.gms.common.ConnectionResult
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import com.ipati.dev.castleevent.R
import com.ipati.dev.castleevent.adapter.ListEventCalendarAdapter
import com.ipati.dev.castleevent.model.EventDetailModel
import com.ipati.dev.castleevent.model.GoogleCalendar.CalendarFragment.CalendarManager
import com.ipati.dev.castleevent.utill.*
import kotlinx.android.synthetic.main.activity_calendar_fragment.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CalendarFragment : Fragment(), View.OnClickListener {
    private var REQUEST_ACCOUNT: Int = 1111
    private lateinit var mCalendarManager: CalendarManager
    private lateinit var mSharePreferenceManager: SharePreferenceGoogleSignInManager
    private lateinit var mListEventDateClick: List<com.github.sundeepk.compactcalendarview.domain.Event>
    private lateinit var mCustomAnimationHeightCollapse: CustomHeightViewCollapse
    private lateinit var mCustomAnimationHeightExpanded: CustomHeightViewExpanded
    private lateinit var mCustomAnimationHeightExpandedCalendar: CustomHeightViewExpandedCalendar
    private lateinit var mCustomAnimationHeightCollapseCalendar: CustomHeightViewCollapseCalendar
    private lateinit var mListEventCalendarAdapter: ListEventCalendarAdapter
    private lateinit var monthDefault: String
    private lateinit var monthScroll: String
    private lateinit var dateScroll: String
    private lateinit var mSimpleDateFormatDateTime: SimpleDateFormat
    private lateinit var mSimpleDateFormatNickNameDate: SimpleDateFormat
    private lateinit var mSimpleDateFormatDateOfYear: SimpleDateFormat
    private lateinit var mSimpleDateFormat: SimpleDateFormat
    private lateinit var eventDetailModel: EventDetailModel
    private lateinit var mCalendarToday: Calendar
    private lateinit var mDateCurrent: Date
    private var mListItemShow: ArrayList<EventDetailModel> = ArrayList()
    private var mListItemEvent: ArrayList<EventDetailModel> = ArrayList()

    private var statusCodeGoogleApiAvailability: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCalendarManager = CalendarManager(context)
        mSharePreferenceManager = SharePreferenceGoogleSignInManager(context)

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.activity_calendar_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialCalendar()
        initialRecyclerViewCalendar()

        defaultMonth()
        requestEventGoogleCalendar()
        tv_hind_bt.setOnClickListener { hindView: View -> onClick(hindView) }
        im_calendar_today.setOnClickListener { todayView: View -> onClick(todayView) }
        tv_header_month.text = defaultMonth()
        tv_calendar_select_date.text = mCalendarManager.initialCalendar().get(Calendar.DATE).toString()
        tv_title.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }

    private fun initialCalendar() {
        compat_calendar_view.displayOtherMonthDays(true)
        compat_calendar_view.setFirstDayOfWeek(Calendar.WEDNESDAY)
        compat_calendar_view.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            @SuppressLint("SetTextI18n")
            override fun onDayClick(dateClicked: Date?) {
                mListEventDateClick = compat_calendar_view.getEvents(dateClicked)
                mCalendarManager.initialCalendar().time = dateClicked
                if (mListEventDateClick.count() == 0) {
                    initialAnimationExpanded()

                    tv_calendar_detail_event.text = ""
                    tv_calendar_time_ticket.text = ""
                    tv_calendar_select_date.text = mCalendarManager.initialCalendar().get(Calendar.DATE).toString()
                    tv_header_month.text = mCalendarManager.initialCalendar().getDisplayName(Calendar.MONTH
                            , Calendar.LONG, Locale("th"))
                    tv_hind_bt.visibility = View.GONE

                    initialCalendarExpanded()
                    mListItemEvent.clear()
                    mListEventCalendarAdapter.notifyDataSetChanged()

                } else {
                    initialAnimationCollapse()
                    initialCalendarCollapse()

                    mListItemEvent.clear()
                    val dayOfYear = mCalendarManager.initialCalendar().get(Calendar.DATE)
                    val monthOfYear: Int = mCalendarManager.initialCalendar().get(Calendar.MONTH) + 1
                    val yearOfYear = mCalendarManager.initialCalendar().get(Calendar.YEAR)
                    val dateTimeStamp = "0$dayOfYear/0$monthOfYear/$yearOfYear"
                    tv_hind_bt.visibility = View.VISIBLE

                    for ((title, timeEventStart, timeEventEnd, timeDayOfYear, timeMonthDate, timeDateEvent) in mListItemShow) {
                        if (timeDateEvent == dateTimeStamp) {
                            eventDetailModel = EventDetailModel(title, timeEventStart, timeEventEnd, timeDayOfYear, timeMonthDate, timeDateEvent)
                            mListItemEvent.add(eventDetailModel)
                            mListEventCalendarAdapter.notifyDataSetChanged()
                        }
                    }

                    tv_calendar_select_date.text = ""
                    tv_header_month.text = "EventList " + yearOfYear
                }
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                mCalendarManager.initialCalendar().time = firstDayOfNewMonth
                monthScroll = mCalendarManager.initialCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("th"))
                dateScroll = mCalendarManager.initialCalendar().get(Calendar.DATE).toString()
                tv_header_month.text = monthScroll
                tv_calendar_select_date.text = dateScroll
                initialAnimationExpanded()
                initialCalendarExpanded()
            }
        })

        initialCalendarExpanded()
    }

    private fun initialCalendarCollapse() {
        mCustomAnimationHeightCollapseCalendar = CustomHeightViewCollapseCalendar(compat_calendar_view, dpToPx(256)
                , compat_calendar_view.height)
        mCustomAnimationHeightCollapseCalendar.interpolator = AccelerateInterpolator()
        mCustomAnimationHeightCollapseCalendar.duration = 600

        compat_calendar_view.animation = mCustomAnimationHeightCollapseCalendar
        compat_calendar_view.startAnimation(mCustomAnimationHeightCollapseCalendar)
    }

    private fun initialCalendarExpanded() {
        mCustomAnimationHeightExpandedCalendar = CustomHeightViewExpandedCalendar(compat_calendar_view, dpToPx(345), compat_calendar_view.height)
        mCustomAnimationHeightExpandedCalendar.interpolator = AccelerateInterpolator()
        mCustomAnimationHeightExpandedCalendar.duration = 600

        compat_calendar_view.animation = mCustomAnimationHeightExpandedCalendar
        compat_calendar_view.startAnimation(mCustomAnimationHeightExpandedCalendar)
    }

    private fun initialRecyclerViewCalendar() {
        calendar_recycler_list_event.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
        calendar_recycler_list_event.itemAnimator = DefaultItemAnimator()
        mListEventCalendarAdapter = ListEventCalendarAdapter(mListItemEvent)
        calendar_recycler_list_event.adapter = mListEventCalendarAdapter
    }

    private fun dpToPx(dp: Int): Int {
        return dp * (Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun initialAnimationCollapse() {
        mCustomAnimationHeightCollapse = CustomHeightViewCollapse(calendar_bar_app, 100, calendar_bar_app.height)
        mCustomAnimationHeightCollapse.interpolator = AccelerateInterpolator()
        mCustomAnimationHeightCollapse.duration = 500
        calendar_bar_app.animation = mCustomAnimationHeightCollapse
        calendar_bar_app.startAnimation(mCustomAnimationHeightCollapse)
    }

    private fun initialAnimationExpanded() {
        mCustomAnimationHeightExpanded = CustomHeightViewExpanded(calendar_bar_app, dpToPx(312), calendar_bar_app.height)
        mCustomAnimationHeightExpanded.interpolator = AccelerateInterpolator()
        mCustomAnimationHeightExpanded.duration = 600
        calendar_bar_app.animation = mCustomAnimationHeightExpanded
        calendar_bar_app.startAnimation(mCustomAnimationHeightExpanded)
    }


    private fun defaultMonth(): String {
        monthDefault = mCalendarManager.initialCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        return monthDefault
    }


    private fun requestEventGoogleCalendar() {
        if (!mGoogleServiceApiAvailabilityEnable()) {
            statusCodeGoogleApiAvailability = mCalendarManager.initialGoogleApiAvailability().isGooglePlayServicesAvailable(context)
            if (mCalendarManager.initialGoogleApiAvailability().isUserResolvableError(statusCodeGoogleApiAvailability!!)) {
                mCalendarManager.onShowDialogAlertGoogleService("GoogleServiceAvailability", statusCodeGoogleApiAvailability.toString()).show()
            }
        } else if (mCalendarManager.initialGoogleAccountCredential().selectedAccountName == null) {
            if (mSharePreferenceManager.defaultSharePreferenceManager() != null) {
                mCalendarManager.initialGoogleAccountCredential().selectedAccountName = mSharePreferenceManager.defaultSharePreferenceManager()
                MakeRequestTask(mGoogleCredentialAccount = mCalendarManager.initialGoogleAccountCredential()).execute()
            } else {
                startActivityForResult(mCalendarManager.initialGoogleAccountCredential().newChooseAccountIntent(), REQUEST_ACCOUNT)
            }
        } else {
            MakeRequestTask(mGoogleCredentialAccount = mCalendarManager.initialGoogleAccountCredential()).execute()
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.tv_hind_bt -> {
                val dayOfYear = mCalendarManager.initialCalendar().get(Calendar.DATE)
                tv_header_month.text = mCalendarManager.initialCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("th"))
                tv_calendar_select_date.text = "$dayOfYear"
                initialAnimationExpanded()
                initialCalendarExpanded()
            }

            R.id.im_calendar_today -> {
                mCalendarToday = Calendar.getInstance()
                mCalendarToday.timeZone = TimeZone.getDefault()

                mDateCurrent = Date(mCalendarToday.timeInMillis)
                tv_calendar_select_date.text = mCalendarToday.get(Calendar.DATE).toString()
                tv_header_month.text = mCalendarToday.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("th"))
                compat_calendar_view.setCurrentDate(mDateCurrent)

                initialAnimationExpanded()
                initialCalendarExpanded()
            }
        }
    }

    private fun mGoogleServiceApiAvailabilityEnable(): Boolean {
        return mCalendarManager.initialGoogleApiAvailability().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ACCOUNT -> {
                mCalendarManager.initialGoogleAccountCredential().selectedAccountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                mSharePreferenceManager.sharePreferenceManager(data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
                requestEventGoogleCalendar()
            }
        }
    }


    companion object {
        fun newInstance(): CalendarFragment {
            return CalendarFragment().apply { arguments = Bundle() }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class MakeRequestTask(mGoogleCredentialAccount: GoogleAccountCredential) : AsyncTask<Void, Void, List<Event>>() {
        private var mService: com.google.api.services.calendar.Calendar? = null
        private var mListError: Exception? = null
        private lateinit var mDateTimeNow: DateTime
        private lateinit var eventListString: ArrayList<String>
        private lateinit var mEvents: Events
        private lateinit var mEventCalendar: com.github.sundeepk.compactcalendarview.domain.Event
        private lateinit var mListEvent: List<Event>
        private lateinit var mItemEvent: EventDetailModel
        private lateinit var mDateMonth: Date
        private lateinit var mDateFormatStart: String
        private lateinit var mDateFormatEnd: String
        private lateinit var mDateMonthFormat: String

        private var mDateStart: Date? = null
        private var mDateEnd: Date? = null
        private var transport: HttpTransport = AndroidHttp.newCompatibleTransport()
        private var jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()

        init {
            mService = com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, mGoogleCredentialAccount)
                    .setApplicationName("Google Calendar Android QuickStart")
                    .build()
        }

        override fun doInBackground(vararg p0: Void?): List<Event>? {
            return try {
                getDataFromApi()
            } catch (e: Exception) {
                mListError = e
                cancel(true)
                null
            }
        }

        private fun getDataFromApi(): List<Event> {
            mDateTimeNow = DateTime(System.currentTimeMillis())
            eventListString = ArrayList()
            mEvents = mService?.events()?.list("primary")
                    ?.setMaxResults(10)
                    ?.setTimeMin(mDateTimeNow)
                    ?.setOrderBy("startTime")
                    ?.setSingleEvents(true)
                    ?.execute()!!

            mListEvent = mEvents.items
            return mListEvent
        }

        override fun onPreExecute() {
            super.onPreExecute()
            Log.d("AsyncTaskPre", "start")
        }

        override fun onPostExecute(result: List<Event>?) {
            super.onPostExecute(result)
            Toast.makeText(context, "Read Event", Toast.LENGTH_SHORT).show()
            for (items in result!!) {
                //Todo: Convert Start Or End Time
                mSimpleDateFormat = SimpleDateFormat("HH.mm", Locale("th"))
                mSimpleDateFormatDateTime = SimpleDateFormat("dd/MM/yyyy", Locale("th"))
                mSimpleDateFormatNickNameDate = SimpleDateFormat("MMM", Locale("th"))
                mSimpleDateFormatDateOfYear = SimpleDateFormat("d", Locale("th"))

                if (items.start != null) {
                    mDateStart = Date(items.start.dateTime.value)
                    mDateFormatStart = mSimpleDateFormat.format(mDateStart)

                    mDateEnd = Date(items.end.dateTime.value)
                    mDateFormatEnd = mSimpleDateFormat.format(mDateEnd)

                    mDateMonth = Date(items.start.dateTime.value)
                    mDateMonthFormat = mSimpleDateFormatDateTime.format(mDateMonth)

                    mItemEvent = EventDetailModel(items.summary, mDateFormatStart, mDateFormatEnd, mSimpleDateFormatDateOfYear.format(mDateMonth), mSimpleDateFormatNickNameDate.format(mDateMonth), mDateMonthFormat)
                    mListItemShow.add(mItemEvent)


                    //Todo: AddEvent To Calendar
                    mEventCalendar = com.github.sundeepk.compactcalendarview.domain
                            .Event(ContextCompat.getColor(context, R.color.colorEvent)
                                    , items.start.dateTime.value, items)

                    compat_calendar_view.addEvent(mEventCalendar, true)
                }
            }
        }

        override fun onCancelled() {
            super.onCancelled()
            if (mListError != null) {
                when (mListError) {
                    is GooglePlayServicesAvailabilityIOException -> {
                        Log.d("AsyncTaskError", "GooglePlayServicesAvailability")
                    }
                    is UserRecoverableAuthIOException -> {
                        Log.d("AsyncTaskError", "UserRecoverable")
                    }
                    else -> {
                        Log.d("AsyncTaskError", mListError.toString())
                    }
                }
            }
        }
    }

}
