package hello.notify

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateCheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result = Result.success()
}
