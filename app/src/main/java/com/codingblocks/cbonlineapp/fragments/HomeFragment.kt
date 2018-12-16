package com.codingblocks.cbonlineapp.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.Utils.retrofitCallback
import com.codingblocks.cbonlineapp.adapters.CourseDataAdapter
import com.codingblocks.cbonlineapp.database.*
import com.codingblocks.cbonlineapp.ui.HomeFragmentUi
import com.codingblocks.onlineapi.Clients
import com.codingblocks.onlineapi.models.Runs
import com.ethanhua.skeleton.Skeleton
import com.ethanhua.skeleton.SkeletonScreen
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.ctx
import kotlin.concurrent.thread

class HomeFragment : Fragment(), AnkoLogger {

    private lateinit var database: AppDatabase
    private lateinit var courseDataAdapter: CourseDataAdapter
    lateinit var skeletonScreen: SkeletonScreen
    lateinit var courseDao: CourseDao
    lateinit var courseWithInstructorDao: CourseWithInstructorDao
    lateinit var instructorDao: InstructorDao
    lateinit var runDao: CourseRunDao


    val ui = HomeFragmentUi<Fragment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ui.createView(AnkoContext.create(ctx, this))
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseDataAdapter = CourseDataAdapter(ArrayList(), activity!!)

        database = AppDatabase.getInstance(context!!)

        courseDao = database.courseDao()
        instructorDao = database.instructorDao()
        runDao = database.courseRunDao()

        courseWithInstructorDao = database.courseWithInstructorDao()

        ui.rvCourses.layoutManager = LinearLayoutManager(ctx)
        ui.rvCourses.adapter = courseDataAdapter

        skeletonScreen = Skeleton.bind(ui.rvCourses)
                .adapter(courseDataAdapter)
                .shimmer(true)
                .angle(20)
                .frozen(true)
                .duration(1200)
                .count(4)
                .load(R.layout.item_skeleton_course_card)
                .show()

        courseDao.getCourses().observe(this, Observer<List<Course>> {
            if (it.isNotEmpty()) {
                skeletonScreen.hide()
                info { "heelo" + it[0].title }
                info { "heelo" + it[0].courseRun.crPrice }


            }
        })

        fetchRecommendedCourses()

    }

    private fun fetchRecommendedCourses() {


        Clients.onlineV2JsonApi.getRecommendedCourses().enqueue(retrofitCallback { t, resp ->
            resp?.body()?.let {
                //                courseDataAdapter.setData(it)
//                skeletonScreen.hide()

                for (myCourses in it) {

                    //calculate top run
                    val currentRuns: ArrayList<Runs> = arrayListOf()
                    for (i in 0 until myCourses.runs!!.size) {
                        if (myCourses.runs!![i].enrollmentStart!!.toLong() < (System.currentTimeMillis() / 1000) && myCourses.runs!![i].enrollmentEnd!!.toLong() > (System.currentTimeMillis() / 1000))
                            currentRuns.add(myCourses.runs!![i])
                    }

                    currentRuns.sortWith(Comparator { o1, o2 -> java.lang.Long.compare(o2.price!!.toLong(), o1.price!!.toLong()) })
                    val course = myCourses.run {
                        Course(
                                id ?: "",
                                title ?: "",
                                subtitle ?: "",
                                logo ?: "",
                                summary ?: "",
                                promoVideo ?: "",
                                difficulty ?: "",
                                reviewCount ?: 0,
                                rating ?: 0f,
                                slug ?: "",
                                coverImage ?: "",
                                "",
                                updatedAt,
                                0.0,
                                "",
                                CourseRun(currentRuns[0].id ?: "", "",
                                        currentRuns[0].name ?: "", currentRuns[0].description ?: "",
                                        currentRuns[0].start ?: "", currentRuns[0].end ?: "",
                                        currentRuns[0].price ?: "", currentRuns[0].mrp ?: "",
                                        myCourses.id ?: "", currentRuns[0].updatedAt ?: ""

                                ))
                    }
                    thread {
                        courseDao.insert(course)
                        //Add CourseInstructors
                        for (i in myCourses.instructors!!) {
                            instructorDao.insert(Instructor(i.id ?: "", i.name ?: "",
                                    i.description ?: "", i.photo ?: "",
                                    "", "", myCourses.id))
                        }
                    }

                }
            }
        })
    }


}
