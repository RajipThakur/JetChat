package com.example.JetChat

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import com.example.JetChat.data.CHATS
import com.example.JetChat.data.ChatData
import com.example.JetChat.data.ChatUser
import com.example.JetChat.data.Event
import com.example.JetChat.data.MESSAGE
import com.example.JetChat.data.Message
import com.example.JetChat.data.STATUS
import com.example.JetChat.data.Status
import com.example.JetChat.data.USER_NODE
import com.example.JetChat.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.Exception
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LCViewModel @Inject constructor(
    val auth: FirebaseAuth,
    var db: FirebaseFirestore,
    val storage: FirebaseStorage
) : ViewModel() {

    var inProgress = mutableStateOf(false)
    var inProgressChats = mutableStateOf(false)
    val eventMutableState = mutableStateOf<Event<String>?>(null)
    var signIn = mutableStateOf(false)
    var userData = mutableStateOf<UserData?>(null)
    var chats = mutableStateOf<List<ChatData>>(listOf())
    val chatMessage = mutableStateOf<List<Message>>(listOf())
    val inProgressChatMessage = mutableStateOf(false)
    var currentChatMessageListener: ListenerRegistration? = null
    val status = mutableStateOf<List<Status>>(listOf())
    val inProgressStatus = mutableStateOf(false)



    init{

        val currentUser = auth.currentUser
        signIn.value = currentUser != null
        currentUser?.uid?.let {
            getUserData(it)
        }

    }

    fun populateMessages(chatId: String) {
        inProgressChatMessage.value = true
        currentChatMessageListener = db.collection(CHATS).document(chatId).collection(MESSAGE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)
                }

                if (value != null) {
                    chatMessage.value = value.documents.mapNotNull {
                        it.toObject<Message>()
                    }.sortedBy { it.timestamp }

                    inProgressChatMessage.value = false
                }

            }
    }

    fun depopulateMessage() {
        chatMessage.value = listOf()
        currentChatMessageListener = null
    }


    fun populateChats() {

        inProgressChats.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)

            }

            if (value != null) {
                chats.value = value.documents.mapNotNull {
                    it.toObject<ChatData>()
                }
                inProgressChats.value = false
            }
        }
    }


    fun onSendReply(chatId: String, message: String) {
        val time = Calendar.getInstance().time.toString()
        val msg = Message(userData.value?.userId, message, time)
        db.collection(CHATS).document(chatId).collection(MESSAGE).document().set(msg)
    }

    fun signUp(name: String, number: String, email: String, password: String) {
        inProgress.value = true
        if (name.isEmpty() or number.isEmpty() or email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill all fields")
            return
        }

        inProgress.value = true
        db.collection(USER_NODE).whereEqualTo("number", number).get().addOnSuccessListener {
            if (it.isEmpty) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        signIn.value = true
                        createOrUpdateProfile(name, number)
                    } else {
                        handleException(it.exception, customMessage = "Sign Up failed")
                    }
                }
            } else {
                handleException(customMessage = "number Already Exists")
                inProgress.value = false
            }
        }


        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                signIn.value = true
                createOrUpdateProfile(name, number)
            } else {
                handleException(it.exception, customMessage = "Sign Up failed")
            }
        }
    }


    fun loginIn(email: String, password: String) {
        if (email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill the all details")
            return
        } else {
            inProgress.value = true
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        signIn.value = true
                        inProgress.value = false

                        auth.currentUser?.uid?.let {
                            getUserData(it)
                        }

                    } else {
                        handleException(exception = it.exception, customMessage = "Login failed")
                    }
                }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        inProgress.value = true
        val storageRef = storage.reference
        uploadImage(uri) {
            createOrUpdateProfile(imageurl = it.toString())
        }
    }

    fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProgress.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("image/$uuid")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl

            result?.addOnSuccessListener(onSuccess)
            inProgress.value = false

        }
            .addOnFailureListener {
                handleException(it)
            }
    }

//    fun createOrUpdateProfile(name:String?=null,number: String?=null,imageurl:String?=null){
//        var uid=auth.currentUser?.uid
//
//        val userData=UserData(
//            userId = uid,
//            name=name?:userData.value?.name,
//            number =number?:userData.value?.number,
//            imageUrl=imageurl?:userData.value?.imageUrl
//        )
//
//        uid?.let{
//            inProgress.value=true
//            db.collection(USER_NODE).document(uid).get().addOnSuccessListener {
//                if(it.exists()){
//                    //update user data
//                }else{
//                    db.collection(USER_NODE).document(uid).set(userData)
//                    inProgress.value=false
//                    getUserData(uid)
//                }
//            }
//                .addOnFailureListener {
//                    handleException(it, " Cannot Retrieve User")
//                }
//
//
//        }
//    }
//
//    private fun getUserData(uid:String) {
//        inProgress.value=true
//
//        db.collection(USER_NODE).document(uid).addSnapshotListener{
//            value,error->
//
//            if(error!=null){
//                handleException(error,"Can not Retreive User")
//            }
//
//            if(value!=null){
//                var user=value.toObject<UserData>()
//                userData.value=user
//                inProgress.value=false
//            }
//        }
//    }

    fun createOrUpdateProfile(
        name: String? = null,
        number: String? = null,
        imageurl: String? = null
    ) {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            // Fetch existing user data if not already fetched
            if (userData.value == null) {
                getUserData(uid)
            }

            // Create new user data
            val userData = UserData(
                userId = uid,
                name = name ?: userData.value?.name,
                number = number ?: userData.value?.number,
                imageUrl = imageurl ?: userData.value?.imageUrl
            )

            inProgress.value = true

            db.collection(USER_NODE).document(uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Update user data
                    db.collection(USER_NODE).document(uid).set(userData).addOnSuccessListener {
                        inProgress.value = false
                        getUserData(uid)
                    }.addOnFailureListener {
                        handleException(it, "Cannot update user data")
                        inProgress.value = false
                    }
                } else {
                    // Set new user data
                    db.collection(USER_NODE).document(uid).set(userData).addOnSuccessListener {
                        inProgress.value = false
                        getUserData(uid)
                    }.addOnFailureListener {
                        handleException(it, "Cannot set user data")
                        inProgress.value = false
                    }
                }
            }.addOnFailureListener {
                handleException(it, "Cannot retrieve user")
                inProgress.value = false
            }
        } else {
            handleException(Exception("User is not authenticated"), "User is not authenticated")
        }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true

        db.collection(USER_NODE).document(uid).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error, "Cannot retrieve user")
                inProgress.value = false
                return@addSnapshotListener
            }

            if (value != null) {
                val user = value.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                populateChats()
                populateStatuses()
            } else {
                inProgress.value = false
            }
        }
    }


    fun handleException(exception: Exception? = null, customMessage: String = "") {
        Log.e("LiveChatApp", "live chat exception ", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isNullOrEmpty()) errorMsg else customMessage
        eventMutableState.value = Event(message)
        inProgress.value = false
    }


    fun logout() {
        auth.signOut()
        signIn.value = false
        userData.value = null
        eventMutableState.value = Event("Logged Out")
    }

    fun onAddChat(number: String) {
        if (number.isEmpty() or !number.isDigitsOnly()) {
            handleException(customMessage = "Number must Contain Digit Only")
        } else {
            db.collection(CHATS).where(
                Filter.or(
                    Filter.and(
                        Filter.equalTo("User1.number", number),
                        Filter.equalTo("User2.number", userData.value?.number)
                    ),
                    Filter.and(
                        Filter.equalTo("User1.number", userData.value?.number),
                        Filter.equalTo("User2.number", number)
                    )

                )
            ).get().addOnSuccessListener {
                if (it.isEmpty) {
                    db.collection(USER_NODE).whereEqualTo("number", number).get()
                        .addOnSuccessListener {
                            if (it.isEmpty) {
                                handleException(customMessage = "number not found")
                            } else {
                                val chatPartner = it.toObjects<UserData>()[0]
                                val id = db.collection(CHATS).document().id
                                val chat = ChatData(
                                    chatId = id,
                                    ChatUser(
                                        userData.value?.userId,
                                        userData.value?.name,
                                        userData.value?.imageUrl,
                                        userData.value?.number
                                    ),
                                    ChatUser(
                                        chatPartner.userId,
                                        chatPartner.name,
                                        chatPartner.imageUrl,
                                        chatPartner.number
                                    )
                                )

                                db.collection(CHATS).document(id).set(chat)
                            }
                        }.addOnFailureListener {
                            handleException(it)
                        }

                } else {
                    handleException(customMessage = "Chat Already Exists")
                }
            }
        }
    }

    fun uploadStatus(uri: Uri) {
        uploadImage(uri) {
            createStatus(it.toString())
        }
    }

    fun createStatus(imageUrl: String) {
        val newStatus = Status(
            ChatUser(
                userData.value?.userId,
                userData.value?.name,
                userData.value?.imageUrl,
                userData.value?.number
            ),
            imageUrl,
            System.currentTimeMillis()
        )

        db.collection(STATUS).document().set(newStatus)
    }

    fun populateStatuses() {

        val timeDlta=24L*60*60*1000
        val cutOff=System.currentTimeMillis()-timeDlta
        inProgressStatus.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId),
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)
            }

            if (value != null) {
                val currentConnections = arrayListOf(userData.value?.userId)
                val chats = value.toObjects<ChatData>()

                chats.forEach { chat ->
                    if (chat.user1.userId == userData.value?.userId) {
                        currentConnections.add(chat.user2.userId)
                    } else {
                        currentConnections.add(chat.user1.userId)
                    }
                }

                db.collection(STATUS).whereGreaterThan("timestamp",cutOff).whereIn("user.userId", currentConnections)
                    .addSnapshotListener {
                                         value, error ->
                        if(error!=null){
                            handleException(error)
                        }

                        if(value!=null){
                            status.value=value.toObjects()
                            inProgressStatus.value=false
                        }

                    }
            }


        }
    }


}


