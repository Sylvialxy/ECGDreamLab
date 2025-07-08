package com.liuxinyu.neurosleep.feature.home.experiment

class ExperimentListActivity /*: AppCompatActivity() {
    private lateinit var binding: ActivityExperimentListBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: ExperimentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExperimentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 RecyclerView
        adapter = ExperimentAdapter()
        binding.recyclerViewExperiments.adapter = adapter
        binding.recyclerViewExperiments.layoutManager = LinearLayoutManager(this)

        // 获取 Token
        val token = AuthManager.getToken(this) ?: run {
            showToast("请重新登录")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 加载数据
        lifecycleScope.launch {
            val result = viewModel.getExperiments(token)
            if (result.code == 200) {
                result.data?.let { experiments ->
                    adapter.submitList(experiments)
                }
            } else {
                showToast(result.message ?: "加载失败")
            }
        }
    }
}*/