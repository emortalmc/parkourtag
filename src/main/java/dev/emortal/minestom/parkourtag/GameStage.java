package dev.emortal.minestom.parkourtag;

public enum GameStage {
    /**
     * Before people have been put in spawn positions, waiting for players to join
     */
    PRE_GAME,
    /**
     * Countdown before the tagger is released
     */
    TAGGER_COUNTDOWN,
    /**
     * The game is live, players are running
     */
    LIVE,
    /**
     * The game has ended, players are in the victory screen
     */
    VICTORY
}
