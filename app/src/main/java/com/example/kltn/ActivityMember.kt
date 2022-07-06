package com.example.kltn

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kltn.fragment.FragmentMember
import com.example.kltn.models.UserModel
import com.example.kltn.services.GroupService
import com.example.kltn.services.UserGroupService
import com.example.kltn.services.UserService
import com.example.kltn.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView

class ActivityMember : AppCompatActivity() {
    private lateinit var btnMemberBack: TextView
    private lateinit var btnMemberAdd: TextView
    private val selectedMember = ArrayList<UserModel>()
    private val selectedMemberAdapter =
        AdapterAddMember({ view, member -> deleteMember(view, member) }, { position, member ->
            updateRolesUser(position, member)
        })
    private lateinit var fragmentMember: FragmentMember
    private lateinit var fragmentSchedule: FragmentSchedule

    private var mTabLayoutSelected = TAB_1

    private var groupId: Int = 0

    private var userId: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member)
        var bundle = intent.extras
        if (bundle != null) {
            groupId = bundle!!.getInt(Constants.GROUD_ID)
            userId = bundle?.getInt(Constants.USER_ID)
        }

        btnMemberBack = findViewById(R.id.btnStaffScheduleBack)
        btnMemberBack.setOnClickListener {
            finish()
        }
        btnMemberAdd = findViewById(R.id.btnMemberAdd)
        btnMemberAdd.setOnClickListener {
            try {

                if (mTabLayoutSelected == TAB_1) {
                    addEventGroup()
                } else {
                    addMemberForGroup()
                }

            } catch (ex: Exception) {
                Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        val fragmentManager: FragmentManager = supportFragmentManager
        fragmentMember = FragmentMember()
        fragmentSchedule = FragmentSchedule()
        var fragmentBundle = Bundle()
        fragmentBundle.putInt(Constants.USER_ID, userId)
        fragmentBundle.putInt(Constants.GROUD_ID, groupId)
        fragmentBundle.putBoolean(Constants.IS_ADD_EVENT_GROUP, true)
        fragmentMember.arguments = fragmentBundle
        fragmentSchedule.arguments = fragmentBundle

        fragmentManager.beginTransaction()
            .add(R.id.fragmentGroupContainer, fragmentSchedule, "Lịch").commit()
        var navbar = findViewById<BottomNavigationView>(R.id.groupNavBar)
        navbar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_4 -> {
                    mTabLayoutSelected = TAB_1
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragmentGroupContainer, fragmentSchedule, "Lịch").commit();
                }
                R.id.nav_5 -> {
                    mTabLayoutSelected = TAB_2
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragmentGroupContainer, fragmentMember, "Thành viên")
                        .commit();
                }
            }
            true
        }
    }

    private fun addMemberForGroup() {
        var dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_addmember)
        var btnAddMemberClose = dialog.findViewById<Button>(R.id.btnAddMemberClose)
        var btnAddMemberSave = dialog.findViewById<Button>(R.id.btnAddMemberSave)
        var txtAddMemberSearch = dialog.findViewById<AutoCompleteTextView>(R.id.txtAddMemberSearch)
        var rcvAddMember = dialog.findViewById<RecyclerView>(R.id.rcvAddMember)
        rcvAddMember.adapter = selectedMemberAdapter
        val listResultMember = ArrayList<UserModel>()
        val listResultMember2 = ArrayList<UserModel>()
        val memberAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.select_dialog_item, ArrayList<String>())
        txtAddMemberSearch.setAdapter(memberAdapter)
        txtAddMemberSearch.threshold = 2
        txtAddMemberSearch.setOnItemClickListener { _, _, position, _ ->
            selectedMember.add(listResultMember2[position])
            selectedMemberAdapter.submitList(selectedMember.toMutableList())
            txtAddMemberSearch.setText("")
            txtAddMemberSearch.clearFocus()
        }
        txtAddMemberSearch.doBeforeTextChanged { _, _, _, _ ->
            listResultMember2.clear()
            for (item in listResultMember) listResultMember2.add(item)
        }
        txtAddMemberSearch.doAfterTextChanged {
            Log.d("Debug:", "doAfterTextChanged")
            if (it.toString().length >= txtAddMemberSearch.threshold) {
                Thread {
                    var selectedUsers = ArrayList<Int>()
                    for (item in selectedMember) {
                        if (selectedUsers.contains(item.id)) continue
                        selectedUsers.add(item.id)
                    }
                    var a = UserService.SearchWithout(
                        it.toString(),
                        selectedUsers
                    )
                    var searchParticipants = a.second?.let {
                        it.toCollection(ArrayList<UserModel>())
                    }
                    listResultMember.clear()
                    searchParticipants?.let {
                        for (item in it) listResultMember.add(item)
                    }
                    memberAdapter.clear()
                    for (user in listResultMember) {
                        memberAdapter.add(user.username + " - " + user.lastname + " " + user.firstname)
                    }

                    memberAdapter.notifyDataSetChanged()
                }.start()
            }
        }
        btnAddMemberClose.setOnClickListener {
            dialog.dismiss()
        }
        btnAddMemberSave.setOnClickListener {
            Thread {
                val resultAddUser = UserGroupService.addUser(selectedMember, groupId)
                runOnUiThread {
                    Toast.makeText(this, resultAddUser.first, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }.start()
        }
        var window = dialog.window
        var wlp = window?.attributes
        wlp?.gravity = Gravity.BOTTOM
        window?.attributes = wlp
        selectedMember.clear()
        selectedMemberAdapter.submitList(selectedMember.toMutableList())
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }


    private fun addEventGroup() {
        val intent = Intent(this, ActivityEditEvent::class.java)
        var b = Bundle()
        b.putInt(Constants.USER_ID, userId)
        b.putInt(Constants.GROUD_ID, groupId)
        b.putInt(Constants.EVENT_ID, 0)
        b.putBoolean(Constants.IS_ADD_EVENT_GROUP, true)
        intent.putExtras(b)
        startActivity(intent)
    }


    fun onMemberClicked(user: UserModel) {
        var intent = Intent(this, ActivityStaffSchedule::class.java).apply {
            putExtra(Constants.GROUD_ID, groupId)
            putExtra(Constants.USER_ID, user.id)
        }
        startActivity(intent)
    }

    private fun deleteMember(view: View?, user: UserModel) {
        var newList = selectedMember.filter { !(it.id == user.id) }
        selectedMember.clear()
        for (item in newList) selectedMember.add(item)
        selectedMemberAdapter.submitList(selectedMember.toMutableList())
    }

    private fun updateRolesUser(position: Int, user: UserModel) {
        selectedMember[position].roles = user.roles
        selectedMemberAdapter.submitList(selectedMember.toMutableList())
    }

    companion object {
        private const val TAB_1 = 0
        private const val TAB_2 = 1
    }
}