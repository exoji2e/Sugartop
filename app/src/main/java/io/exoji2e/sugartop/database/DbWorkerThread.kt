package io.exoji2e.sugartop.database

import android.os.Handler
import android.os.HandlerThread

class DbWorkerThread(threadName: String) : HandlerThread(threadName) {

    private lateinit var mWorkerHandler: Handler

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mWorkerHandler = Handler(looper)
    }

    fun postTask(task: Runnable) {
        mWorkerHandler.post(task)
    }
    companion object {
        var mThread : DbWorkerThread? = null
        private fun getInstance() : DbWorkerThread {
            if(mThread == null){
                synchronized(DbWorkerThread::class.java){
                    if(mThread == null) {
                        mThread = DbWorkerThread("dbWorker")
                        mThread!!.start()
                        mThread!!.onLooperPrepared()
                    }
                }
            }
            return mThread!!
        }
        fun postTask(task : Runnable) {
            getInstance().postTask(task)
        }
    }
}