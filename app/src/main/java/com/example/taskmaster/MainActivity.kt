package com.example.taskmaster

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Query
import com.example.taskmaster.adapters.TaskRVVBListAdapter
import com.example.taskmaster.adapters.TaskRVViewBindingAdapter
import com.example.taskmaster.databinding.ActivityMainBinding
import com.example.taskmaster.models.Task
import com.example.taskmaster.utils.Status
import com.example.taskmaster.utils.StatusResult
import com.example.taskmaster.utils.StatusResult.*
import com.example.taskmaster.utils.clearEditText
import com.example.taskmaster.utils.hideKeyBoard
import com.example.taskmaster.utils.longToastShow
import com.example.taskmaster.utils.setupDialog
import com.example.taskmaster.utils.validateEditText
import com.example.taskmaster.viewmodels.TaskViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val mainBinding :ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val taskAddDialog : Dialog by lazy {
        Dialog(this,R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.task_add_dialog)
        }
    }

    private val taskUpdateDialog : Dialog by lazy {
        Dialog(this,R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.task_update_dialog)
        }
    }

    private val loadingDialog : Dialog by lazy {
        Dialog(this,R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    private val taskViewModel : TaskViewModel by lazy {
        ViewModelProvider(this)[TaskViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)



        // Add Tasks
        val closeImgInAdd = taskAddDialog.findViewById<ImageView>(R.id.closeImg)
        closeImgInAdd.setOnClickListener {taskAddDialog.dismiss()}

        val addETTitle = taskAddDialog.findViewById<TextInputEditText>(R.id.addTaskTitle)
        val addETTitleL = taskAddDialog.findViewById<TextInputLayout>(R.id.addTaskTitleL)

        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETTitle, addETTitleL)
            }

        })

        val addETDesc = taskAddDialog.findViewById<TextInputEditText>(R.id.addTaskDisc)
        val addETDescL = taskAddDialog.findViewById<TextInputLayout>(R.id.addTaskDiscL)

        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETDesc, addETDescL)
            }
        })

        mainBinding.taskAddBtn.setOnClickListener {
            clearEditText(addETTitle, addETTitleL)
            clearEditText(addETDesc, addETDescL)
            taskAddDialog.show()
        }

        val saveTaskBtn = taskAddDialog.findViewById<Button>(R.id.saveTaskBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL) && validateEditText(addETDesc, addETDescL)) {
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    Date()
                )
                hideKeyBoard(it)
                taskAddDialog.dismiss()
                taskViewModel.insertTask(newTask)
            }
        }

        //update Tasks
        val updateETTitle = taskUpdateDialog.findViewById<TextInputEditText>(R.id.editTaskTitle)
        val updateETTitleL = taskUpdateDialog.findViewById<TextInputLayout>(R.id.editTaskTitleL)

        updateETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETTitle, updateETTitleL)
            }

        })

        val updateETDesc = taskUpdateDialog.findViewById<TextInputEditText>(R.id.editTaskDisc)
        val updateETDescL = taskUpdateDialog.findViewById<TextInputLayout>(R.id.editTaskDiscL)

        updateETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETDesc, updateETDescL)
            }
        })

        val closeImgInUpdate = taskUpdateDialog.findViewById<ImageView>(R.id.closeImg)
        closeImgInUpdate.setOnClickListener {taskUpdateDialog.dismiss()}

        val updateTaskBtn = taskUpdateDialog.findViewById<Button>(R.id.updateTaskBtn)
        updateTaskBtn.setOnClickListener {
            if (validateEditText(updateETTitle, updateETTitleL) && validateEditText(updateETDesc, updateETDescL)) {
                taskUpdateDialog.dismiss()
                Toast.makeText(this, "Validated!!", Toast.LENGTH_LONG).show()
                loadingDialog.show()
            }
        }

        // Task delete and update
        val taskRVVBListAdapter = TaskRVVBListAdapter{type,position, task ->
            if (type == "delete") {
                taskViewModel
                    .deleteTaskById(task.id)
                //Restore Task
                restoreTask(task)
            }else if (type == "update"){
                updateETTitle.setText(task.title)
                updateETDesc.setText(task.description)
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL) && validateEditText(updateETDesc, updateETDescL)) {
                        val updateTask = Task(
                            task.id,
                            updateETTitle.text.toString().trim(),
                            updateETDesc.text.toString().trim(),
                            Date()
                        )
                        hideKeyBoard(it)
                        taskUpdateDialog.dismiss()
                        taskViewModel.updateTask(updateTask)

                    }
                }
                taskUpdateDialog.show()
            }
        }

        mainBinding.taskRV.adapter = taskRVVBListAdapter
        ViewCompat.setNestedScrollingEnabled(mainBinding.taskRV, false)
        taskRVVBListAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
//                mainBinding.taskRV.smoothScrollToPosition(positionStart)
                mainBinding.nestedScrollView.smoothScrollTo(0, positionStart)
            }
        })
        callGetTaskList(taskRVVBListAdapter)
        callSortByLiveData()
        statusCallback()

        //search part
        callSearch()

    }


    private fun restoreTask(deletedTask: Task){
        val snackbar = Snackbar.make(
            mainBinding.root, "Deleted '${deletedTask.title}'",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("Undo"){
            taskViewModel.insertTask(deletedTask)
        }
        snackbar.show()
    }

    private fun callSearch() {
        mainBinding.searchTitle.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(query: Editable) {
                if (query.toString().isNotEmpty()){
                    taskViewModel.searchTaskList(query.toString())
                }else{
                    callSortByLiveData()
                }
            }
        })

        mainBinding.searchTitle.setOnEditorActionListener{ v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                hideKeyBoard(v)
                return@setOnEditorActionListener true
            }
            false
        }
        callSortByDialog()
    }

    private fun callSortByLiveData(){
        taskViewModel.sortByLiveData.observe(this){
            taskViewModel.getTaskList(it.second, it.first)
        }
    }

    private fun callSortByDialog() {
        var checkedItem = 0   // 2 is default item set
        val items = arrayOf("Title Ascending", "Title Descending","Date Ascending","Date Descending")

        mainBinding.sortImg.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sort By")
                .setPositiveButton("Ok") { _, _ ->
                    when (checkedItem) {
                        0 -> {
                            taskViewModel.setSortBy(Pair("title",true))
                        }
                        1 -> {
                            taskViewModel.setSortBy(Pair("title",false))
                        }
                        2 -> {
                            taskViewModel.setSortBy(Pair("date",true))
                        }
                        else -> {
                            taskViewModel.setSortBy(Pair("date",false))
                        }
                    }
                }
                .setSingleChoiceItems(items, checkedItem) { _, selectedItemIndex ->
                    checkedItem = selectedItemIndex
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun statusCallback() {
        taskViewModel
            .taskLiveData
            .observe(this){
                when (it.status) {
                    Status.LOADING -> {
                        loadingDialog.show()
                    }

                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        when(it.data as StatusResult){
                            Added ->{
                                Log.d("StatusResult","Added")
                            }
                            Deleted ->{
                                Log.d("StatusResult","Deleted")
                            }
                            Updated -> {
                                Log.d("StatusResult","Updated")
                            }
                        }
                        it.message?.let { it1 -> longToastShow(it1) }
                    }

                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        it.message?.let { it1 -> longToastShow(it1) }
                    }
                }
            }
    }

    private fun callGetTaskList(taskRecyclerViewAdapter: TaskRVVBListAdapter) {
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel
                .taskStateFlow
                .collectLatest {
                when (it.status) {
                    Status.LOADING -> {
                        loadingDialog.show()
                    }

                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        it.data?.collect {taskList->
                            taskRecyclerViewAdapter.submitList(taskList)
                        }
                    }

                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        it.message?.let { it1 -> longToastShow(it1) }
                    }
                }
            }
        }
    }
}