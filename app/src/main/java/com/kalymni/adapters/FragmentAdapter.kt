package com.kalymni.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.kalymni.fragments.ChatsFragment
import com.kalymni.fragments.GroupsFragment

class FragmentAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ChatsFragment()
            1 -> GroupsFragment()
            else -> {
                ChatsFragment()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        var title: String? = null
        when (position) {
            0 -> {
                title = "Chats".lowercase()
            }

            1 -> {
                title = "Groups".lowercase()
            }
        }
        return title!!.lowercase()
    }
}