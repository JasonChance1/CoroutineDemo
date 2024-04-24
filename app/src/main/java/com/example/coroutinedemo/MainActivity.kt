package com.example.coroutinedemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.coroutinedemo.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"
    private var _binding: ActivityMainBinding? = null
    private val binding:ActivityMainBinding get() = requireNotNull(_binding) { "The property of binding has been destroyed." }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.doRequestLaunch.setOnClickListener {
            doRequestLaunch()
        }

        binding.doRequestAsync.setOnClickListener {
            doRequestAsync()
        }

        binding.doLaunchAsync.setOnClickListener {
            doLaunchAsync()
        }

        binding.getAllAddress.setOnClickListener {
            getAllAddress()
        }

        binding.getAllAddress.setOnClickListener {
            getAllAddress()
        }

        var count = 0
        binding.reallyRequest.setOnClickListener {
            count++
            doReallyRequest(count)
        }

        binding.intent.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
            finish()

        }
    }

    /**
     * 使用 launch 来异步执行多个协程任务。
     * 请求顺序：A, B, C 同时进行，所有请求完成后执行 D。
     */
    private fun doRequestLaunch() {
        lifecycleScope.launch {
            // 启动协程执行 getA，不需要结果。
            val job = launch {
                getA()
            }
            // 启动协程执行 getB，不需要结果。
            val jobB = launch {
                getB()
            }
            // 启动协程执行 getC，不需要结果。
            val jobC = launch {
                getC()
            }
            // 等待所有任务完成
            job.join()
            jobB.join()
            jobC.join()
            // 执行依赖于前面任务的函数 getD，此处不传递参数
            getD()
        }
    }

    /**
     * 使用 async 来启动协程并获取它们的结果。
     * 请求顺序：A, B, C 同时进行，所有请求完成后，使用结果执行 D。
     */
    private fun doRequestAsync() {
        lifecycleScope.launch {
            // 启动协程并期望返回结果，使用 async
            val job = async {
                getA()
            }
            // 启动协程执行 getB，由于无返回值，结果集中对应 null
            val jobB = async {
                getB()
            }
            // 启动协程并期望返回结果，使用 async
            val jobC = async {
                getC()
            }
            // 等待所有协程完成，并获取结果集
            val result = awaitAll(job, jobB, jobC)
            // 构建结果字符串
            val s = StringBuffer().apply {
                result.forEach {
                    append("$it/")
                }
            }
            // 使用结果执行 getD
            getD(s.toString())
        }
    }

    /**
     * 混合使用 launch 和 async 来控制任务执行和结果获取。
     * 请求顺序：A 和 C 需要结果并行执行，B 无需结果并行执行，完成后使用 A 和 C 的结果执行 D。
     */
    private fun doLaunchAsync() {
        lifecycleScope.launch {
            // 启动协程并期望返回结果，使用 async
            val jobA = async {
                getA()
            }
            // 启动协程执行 getB，不需要结果
            val jobB = launch {
                getB()
            }
            // 启动协程并期望返回结果，使用 async
            val jobC = async {
                getC()
            }

            // 确保 B 任务已经完成
            jobB.join()
            // 等待 A 和 C 完成，并获取结果
            val result = awaitAll(jobA, jobC)
            // 构建结果字符串
            val s = StringBuffer().apply {
                result.forEach {
                    append("$it/")
                }
            }
            // 使用结果执行 getD
            getD(s.toString())
        }
    }

    /**
     * 模拟网络请求A，返回结果为 String。
     */
    private suspend fun getA(): String {
        delay(2000)  // 模拟网络延迟
        Log.e(tag, "getA")
        return "A"
    }

    /**
     * 模拟网络请求B，无返回值。
     */
    private suspend fun getB() {
        delay(3000)  // 模拟网络延迟
        Log.e(tag, "getB")
    }

    /**
     * 模拟网络请求C，返回结果为 Int。
     */
    private suspend fun getC(): Int {
        delay(1500)  // 模拟网络延迟
        Log.e(tag, "getC")
        return 100
    }

    /**
     * 模拟网络请求D，接受参数并返回 String。
     */
    private suspend fun getD(params: String = ""): String {
        delay(800)  // 模拟网络延迟
        Log.e(tag, "getD：$params")
        return "D:$params"
    }

    private suspend fun getCode(): List<Int> {
        delay(100)
        Log.e(tag, "getCode")
        return listOf(1, 2, 3, 4)
    }

    private suspend fun getAddress(code: Int): String {
        delay(100)
        return "地址$code"
    }

    private fun getAllAddress() {
        lifecycleScope.launch {
            val codeList = async { getCode() }.await()
            val addressList = codeList.map {
                async { getAddress(it) }
            }.awaitAll()
            Log.e(tag, "addressList:$addressList")
        }
    }

    private fun doReallyRequest(count: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val a = request(count)
                Log.e(tag, "result:$a")
                binding.result.text = a.toString()
            } catch (e: Exception) {
                Log.e(tag, "请求异常:${e.message}")
            }
        }
    }

    private suspend fun request(count: Int): Int = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            // 模拟网络请求成功和失败
            if (count % 2 == 0) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { continuation.resume(count + 1) }, 2000
                )
            } else {
                continuation.resumeWithException(RuntimeException("请求异常"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

//   fun doRequest(count:Int){
//       viewModelScope.launch(Dispatchers.Main) {
//           try {
//               val a = repository.request(count)
//
//           } catch (e: Exception) {
//               Log.e(tag, "请求异常:${e.message}")
//           }
//       }
//   }

}
/**
 * 通过lifeScope启动的协程，界面结束时不需要手动取消协程
 */