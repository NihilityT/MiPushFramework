package top.trumeet.mipushframework.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.google.gson.Gson
import kotlin.reflect.KClass

fun <T : Any> snapshotStateListSaver(klass: KClass<T>) = listSaver<SnapshotStateList<T>, String>(
    save = { it.map { Gson().toJson(it) } },
    restore = { it.map { Gson().fromJson(it, klass.java) }.toMutableStateList() },
)

fun <T : Any> mutableStateSaver(klass: KClass<T>) = Saver<MutableState<T>, String>(
    save = { Gson().toJson(it.value) },
    restore = { mutableStateOf(Gson().fromJson(it, klass.java)) },
)