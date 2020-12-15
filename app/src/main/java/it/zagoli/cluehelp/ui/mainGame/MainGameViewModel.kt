package it.zagoli.cluehelp.ui.mainGame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.zagoli.cluehelp.ClueHelpApplication
import it.zagoli.cluehelp.R
import it.zagoli.cluehelp.domain.GameObject
import it.zagoli.cluehelp.domain.Player
import it.zagoli.cluehelp.domain.Question
import it.zagoli.cluehelp.extensions.MutableLiveList
import it.zagoli.cluehelp.ui.TempStore
import it.zagoli.cluehelp.ui.NavigationStatus
import timber.log.Timber

/**
 * shared viewModel for MainGameFragment, AddQuestionFragment
 */
class MainGameViewModel(application: Application) : AndroidViewModel(application) {

    // DATA FOR NEW QUESTION
    /**
     * backing property for [navigationAddQuestionToMainGameEvent]
     */
    private val _navigationAddQuestionToMainGameEvent = MutableLiveData<NavigationStatus>()

    /**
     * this event represent the navigation from AddQuestionFragment back to MainGameFragment,
     * so before the navigation happens, we need to add the relative question.
     * Thus, all of the data for the question to be added must be non-null.
     */
    val navigationAddQuestionToMainGameEvent: LiveData<NavigationStatus>
        get() = _navigationAddQuestionToMainGameEvent

    /**
     * [Player] who answered the new question
     */
    var questionQuestioned: Player? = null

    /**
     * room of the new question
     */
    var questionRoom: GameObject? = null

    /**
     * weapon of the new question
     */
    var questionWeapon: GameObject? = null

    /**
     * suspect of the new question
     */
    var questionSuspect: GameObject? = null

    /**
     * [Player] who asked the new question
     */
    var questionQuestioner: Player? = null
    // END OF DATA FOR NEW QUESTION

    /**
     * this function will trigger navigation to main game and adding a new question if the conditions are met.
     */
    fun navigateToMainGameAndAddQuestion() {
        if (questionQuestioner != null &&
            questionSuspect != null &&
            questionWeapon != null &&
            questionRoom != null &&
            questionQuestioned != null
        ) {
            val gameObjectList = mutableListOf(
                Pair(questionSuspect!!, true),
                Pair(questionWeapon!!, true),
                Pair(questionRoom!!,true)
            )
            val question = Question(gameObjectList, questionQuestioner!!, questionQuestioned!!)
            addNewQuestion(question)
            //reset objects
            questionQuestioner = null
            questionSuspect = null
            questionWeapon = null
            questionRoom = null
            questionQuestioned = null
            //navigate
            _navigationAddQuestionToMainGameEvent.value = NavigationStatus.OK
        } else {
            _navigationAddQuestionToMainGameEvent.value = NavigationStatus.IMPOSSIBLE
        }
    }

    /**
     * call after the navigation to main game from add question
     */
    fun questionAddedNavigationComplete() {
        _navigationAddQuestionToMainGameEvent.value = NavigationStatus.DONE
    }

    /**
     * list of players
     */
    val players = TempStore.players

    /**
     * the player that represents nobody. We store it here so it's created only once
     */
    private val nobody =
        Player(getApplication<ClueHelpApplication>().getString(R.string.player_nobody))

    /**
     * an array with the placeholder and nobody player for the spinners
     */
    val playersWithPlaceholderAndNobodyArray: Array<Player>
        get() {
            return mutableListOf(Player("")).let {
                it.addAll(players)
                it.add(nobody)
                it.toTypedArray()
            }
        }

    /**
     * backing property for [gameObjects]
     */
    private val _gameObjects = MutableLiveList<GameObject>()

    /**
     * holds the main list of [GameObject]
     */
    val gameObjects: LiveData<MutableList<GameObject>>
        get() = _gameObjects

    /**
     * container for all the questions
     */
    private val questionList: MutableList<Question> = mutableListOf()

    /**
     * this function performs the calculations after a new owner for one
     * object is discovered.
     * @param gameObject is the object with the new owner
     */
    fun newObjectOwnerDiscovered(gameObject: GameObject) {
        Timber.i("new object owner: ${gameObject.owner?.name} for object ${gameObject.name}")
    }

    /**
     * this function performs the calculations after a new question is added
     * @param question the new question added
     */
    private fun addNewQuestion(question: Question) {
        Timber.i("new question added.")
    }


    init {
        // initialize the list with the previously collected data
        _gameObjects.addAll(TempStore.gameObjects)
    }

}