package it.zagoli.cluehelp.ui.mainGame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.zagoli.cluehelp.BuildConfig
import it.zagoli.cluehelp.ClueHelpApplication
import it.zagoli.cluehelp.R
import it.zagoli.cluehelp.domain.GameObject
import it.zagoli.cluehelp.domain.Player
import it.zagoli.cluehelp.domain.Question
import it.zagoli.cluehelp.extensions.*
import it.zagoli.cluehelp.ui.TempStore
import it.zagoli.cluehelp.ui.NavigationStatus
import timber.log.Timber

/**
 * shared viewModel for MainGameFragment, AddQuestionFragment
 */
class MainGameViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------------------------------NEW QUESTION---------------------------------------------------
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
    var questionAnswers: Player? = null

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
    var questionAsks: Player? = null

    /**
     * this function will trigger navigation to main game and adding a new question if the conditions are met.
     */
    fun navigateToMainGameAndAddQuestion() {
        if (questionAsks != null &&
            questionSuspect != null &&
            questionWeapon != null &&
            questionRoom != null &&
            questionAnswers != null
        ) {
            val gameObjectList = mutableListOf(
                GameObjectWrapper(questionSuspect!!),
                GameObjectWrapper(questionWeapon!!),
                GameObjectWrapper(questionRoom!!)
            )
            val question = Question(gameObjectList, questionAsks!!, questionAnswers!!)
            addNewQuestion(question)
            //reset objects
            questionAsks = null
            questionSuspect = null
            questionWeapon = null
            questionRoom = null
            questionAnswers = null
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

    // -------------------------------------------MAIN GAME--------------------------------------------------

    /**
     * list of players
     */
    val players = TempStore.players

    /**
     * the player that represents nobody. We store it here so it's created only once
     */
    private val nobody = Player(getApplication<ClueHelpApplication>().getString(R.string.player_nobody))

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
     * this function performs the calculations after a new owner for one object is discovered.
     * Only this method updates the screen!
     * @param gameObject is the object with the new owner
     */
    fun newObjectOwnerDiscovered(gameObject: GameObject) {
        Timber.i("new object owner: ${gameObject.owner?.name} for object ${gameObject.name}")
        // this set holds the new other objects that we may discover to process them later.
        val newGameObjectsSet: MutableSet<GameObject> = mutableSetOf()
        questionList.forEachValidQuestion { question ->
            // at this point the gameObject owner should be not null
            if (question.gameObjects.containsGameObject(gameObject) && gameObject.owner != question.answers) {
                question.gameObjects.gowFromGameObject(gameObject).invalidate()
            }
            // if only one valid object in question
            if (question.validObjectsNumber == 1) {
                question.invalidate()
                val newObj = question.firstValidObject
                if (newObj.owner == null) {
                    newObj.owner = question.answers
                    newGameObjectsSet.add(newObj)
                }
            }
        }
        for (newObj in newGameObjectsSet) {
            newObjectOwnerDiscovered(newObj)
        }
    }

    /**
     * container for all the questions
     */
    private val questionList: MutableList<Question> = mutableListOf()

    /**
     * this function performs the calculations after a new question is added
     * @param question the new question added
     */
    private fun addNewQuestion(question: Question) {
        Timber.i("question number ${questionList.size+1} added.")
        questionList.add(question)
        // adding items in players not owned objects list. See flowcharts for better explanation
        players.forFromPlayerUntilPlayer(question.asks, question.answers) { player ->
            player.notOwnedGameObjects.addAll(question.gameObjects.map { gameObjectWrapper -> gameObjectWrapper.gameObject })
        }
        // question contains object owned by questioned player
        for (gameObject in question.gameObjects.map { gameObjectWrapper -> gameObjectWrapper.gameObject }) {
            if (gameObject.owner != null && gameObject.owner == question.answers) {
                question.invalidate()
                checkQuestionsNotOwnedObjects()
                return
            }
        }
        // invalidate objects owner by others
        for (gow in question.gameObjects) {
            if (gow.isValid() && gow.gameObject.owner != null && gow.gameObject.owner != question.answers) {
                gow.invalidate()
            }
        }
        // valid objects == 1?
        if (question.validObjectsNumber == 1) {
            question.invalidate()
            val newGameObject = question.firstValidObject
            newGameObject.owner = question.answers
            newObjectOwnerDiscovered(newGameObject)
        }
        checkQuestionsNotOwnedObjects()
    }

    /**
     * checks all the questions in [questionList]: eliminates from the question
     * objects that aren't owned by the player who answered
     */
    private fun checkQuestionsNotOwnedObjects() {
        questionList.forEachValidQuestion { question ->
            for (gow in question.gameObjects) {
                if (question.answers.notOwnedGameObjects.contains(gow.gameObject)) {
                    gow.invalidate()
                }
            }
            if (question.validObjectsNumber == 1) {
                question.invalidate()
                val newGameObject = question.firstValidObject
                newGameObject.owner = question.answers
                newObjectOwnerDiscovered(newGameObject)
            }
        }
    }

    init {
        // initialize the list with the previously collected data
        _gameObjects.addAll(TempStore.gameObjects)
    }

}