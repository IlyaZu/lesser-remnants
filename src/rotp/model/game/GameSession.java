/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2025 Ilya Zushinskiy
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.game;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import rotp.Rotp;
import rotp.model.empires.Empire;
import rotp.model.galaxy.*;
import rotp.ui.notifications.GNNExpansionEvent;
import rotp.ui.notifications.GNNRankingNoticeCheck;
import rotp.ui.NoticeMessage;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.notifications.GameAlert;
import rotp.ui.notifications.SystemsScoutedNotification;
import rotp.ui.notifications.TurnNotification;
import rotp.ui.sprites.FlightPathSprite;
import rotp.util.Base;

public final class GameSession implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final String BACKUP_DIRECTORY = "backup";
    public static final String SAVEFILE_EXTENSION = ".rotp";
    public static final String RECENT_SAVEFILE = "recent"+SAVEFILE_EXTENSION;
    private static final Object ONE_GAME_AT_A_TIME = new Object();
    private static GameSession instance = new GameSession();
    public static GameSession instance()  { return instance; }

    private static final int MINIMUM_NEXT_TURN_TIME = 500;
    private static Thread nextTurnThread;
    private static volatile boolean suspendNextTurn = false;
    private static final ThreadFactory minThreadFactory = GameSession.minThreadFactory();
    private static ExecutorService smallSphereService = Executors.newSingleThreadExecutor(minThreadFactory);

    private static HashMap<String,Object> vars;
    private static boolean performingTurn;
    private static final List<TurnNotification> notifications = new ArrayList<>();
    private static HashMap<StarSystem, List<String>> systemsToAllocate;
    private static HashMap<String, List<StarSystem>> systemsScouted;
    private static final List<GameAlert> alerts = new ArrayList<>();
    private static int viewedAlerts;

    private IGameOptions options = new MOO1GameOptions();
    private Galaxy galaxy;
    private final GameStatus status = new GameStatus();
    private boolean spyActivity = false;
    
    public GameStatus status()                   { return status; }
    public ExecutorService smallSphereService()  { return smallSphereService; }

    public void pauseNextTurnProcessing(String s)   {
        if (performingTurn) {
            log("Pausing Next Turn: ", s);
            suspendNextTurn = true;
        }
    }
    public void resumeNextTurnProcessing()  {
        log("Resuming Next Turn");
        suspendNextTurn = false;
    }
    public HashMap<StarSystem, List<String>> systemsToAllocate() {
        if (systemsToAllocate == null)
            systemsToAllocate = new HashMap<>();
        return systemsToAllocate;
    }
    public HashMap<String, List<StarSystem>> systemsScouted() {
        if (systemsScouted == null) {
            systemsScouted = new HashMap<>();
            systemsScouted.put("Scouts", new ArrayList<>());
            systemsScouted.put("Allies", new ArrayList<>());
            systemsScouted.put("Astronomers", new ArrayList<>());
        }
        return systemsScouted;
    }
    private List<TurnNotification> notifications() {
        return notifications;
    }
    private HashMap<String,Object> vars() {
        if (vars == null)
            vars = new HashMap<>();
        return vars;
    }
    public GameAlert currentAlert() {
        if (viewedAlerts >= alerts.size())
            return null;
        return alerts.get(viewedAlerts);
    }
    public int viewedAlerts()    { return viewedAlerts; }
    public int numAlerts()       { return alerts.size(); }
    public void addAlert(GameAlert a)  { alerts.add(a); }
    private void clearAlerts() {
        alerts.clear();
        viewedAlerts = 0;
    }
    public void dismissAlert() { viewedAlerts++; }

    public boolean performingTurn()      { return performingTurn; }
    @Override
    public IGameOptions options()        { return options; }
    @Override
    public Galaxy galaxy()               { return galaxy; }
    public void galaxy(Galaxy g)         { galaxy = g; }

    public void enableSpyReport() {
        spyActivity = true;
    }
    public boolean spyActivity()            { return spyActivity; }
    public void addSystemScouted(StarSystem sys) {
        systemsScouted().get("Scouts").add(sys);
    }
    public void addSystemScoutedByAllies(StarSystem sys) {
        systemsScouted().get("Allies").add(sys);
    }
    public void addSystemScoutedByAstronomers(StarSystem sys) {
        systemsScouted().get("Astronomers").add(sys);
    }
    private void clearScoutedSystems() {
        systemsScouted().get("Scouts").clear();
        systemsScouted().get("Allies").clear();
        systemsScouted().get("Astronomers").clear();
    }
    public boolean haveScoutedSystems() {
        for (Collection<StarSystem> systems : systemsScouted().values()) {
            if (!systems.isEmpty())
                return true;
        }
        return false;
    }
    public void addSystemToAllocate(StarSystem sys, String reason) {
        // don't prompt to allocate systems that are in rebellion
        if (sys.isColonized() && sys.colony().inRebellion())
            return;
        
        log("Re-allocate: ", sys.name(), " :", reason);
        if (!systemsToAllocate().containsKey(sys))
            systemsToAllocate().put(sys, new ArrayList<>());

        if (!systemsToAllocate().get(sys).contains(reason))
            systemsToAllocate().get(sys).add(reason);
    }
    public boolean awaitingAllocation(StarSystem sys) {
        return systemsToAllocate().containsKey(sys);
    }
    public void addTurnNotification(TurnNotification notif) {
        notifications().add(notif);
    }
    public void removePendingNotification(String key) {
        List<TurnNotification> notifs = new ArrayList<>(notifications());
        for (TurnNotification notif: notifs) {
            if (notif.key().equals(key))
                notifications.remove(notif);
        }
            
    }
    public void startGame(IGameOptions newGameOptions) {
        stopCurrentGame();
        
        options = newGameOptions;
        startExecutors();
        
        synchronized(ONE_GAME_AT_A_TIME) {
            GalaxyFactory.current().newGalaxy();
            log("Galaxy complete");
            status().startGame();
            clearScoutedSystems();
            systemsToAllocate().clear();
            spyActivity = false;
            galaxy().startGame();
            saveRecentSession(false);
            saveBackupSession(1);
            clearNewGameOptions();
        }
    }
    private void  startExecutors() {
        smallSphereService = Executors.newSingleThreadExecutor();
    }
    private void stopCurrentGame() {
        vars().clear();
        clearAlerts();
        // shut down any threads running from previous game
        smallSphereService().shutdownNow();
    }
    public void exit()                        { System.exit(0); }
    public Object var(String key)             { return vars().get(key); }
    public void var(String key, Object value) { vars().put(key, value); }
    public void removeVar(String key)         { vars().remove(key); }
    public void replaceVarValue(Object prevValue, Object newValue) {
        List<String> keys = new ArrayList<>();
        keys.addAll(vars().keySet());
        for (String key: keys) {
            if (var(key) == prevValue) {
                log("replacing value for session var: ", key);
                var(key, newValue);
            }
        }
    }
    public void nextTurn() {
        if (performingTurn())
            return;

        performingTurn = true;
        nextTurnThread = new Thread(nextTurnProcess());
        nextTurnThread.start();
    }
    public void waitUntilNextTurnCanProceed() {
        while(suspendNextTurn)
            sleep(200);
    }
    public boolean inProgress()  { return status().inProgress(); }
    private Runnable nextTurnProcess() {
        return () -> {
            try {
                performingTurn = true;
                Galaxy gal = galaxy();
                String turnTitle = nextTurnTitle();
                NoticeMessage.setStatus(turnTitle, text("TURN_SAVING"));
                FlightPathSprite.clearWorkingPaths();
                RotPUI.instance().mainUI().saveMapState();
                log("Next Turn - BEGIN: ", str(galaxy.currentTurn()));
                log("Autosaving pre-turn");
                instance.saveRecentSession(false);
                
                long startMs = System.currentTimeMillis();
                systemsToAllocate().clear();
                clearScoutedSystems();
                spyActivity = false;
                clearAlerts();
                RotPUI.instance().repaint();
                processNotifications();
                gal.preNextTurn();
                
                if (!inProgress())
                    return;
               
                // all intra-empire events: civ turns, ship movement, etc
                gal.advanceTime();
                gal.moveShipsInTransit();
                
                gal.events().nextTurn();
                RotPUI.instance().selectMainPanel();

                gal.council().nextTurn();
                GNNRankingNoticeCheck.nextTurn();
                GNNExpansionEvent.nextTurn();
                gal.nextEmpireTurns();
                player().setVisibleShips();
                
                if (!inProgress())
                    return;

                if (processNotifications()) {
                    log("Notifications processed 1 - back to MainPanel");
                    RotPUI.instance().selectMainPanel();
                }
                gal.postNextTurn1();
                if (!inProgress())
                    return;

                if (processNotifications()) {
                    log("Notifications processed 2 - back to MainPanel");
                    RotPUI.instance().selectMainPanel();
                }
                gal.refreshAllEmpireViews();
                gal.postNextTurn2();

                if (!inProgress())
                    return;
                if (processNotifications()) {
                    log("Notifications processed 3 - back to MainPanel");
                    RotPUI.instance().selectMainPanel();
                }
                // all diplomatic fallout: praise, warnings, treaty offers, war declarations
                gal.assessTurn();
                
                if (processNotifications()){
                    log("Notifications processed 4 - back to MainPanel");
                    RotPUI.instance().selectMainPanel();
                }
                gal.makeNextTurnDecisions();

                if (processNotifications()){
                    log("Notifications processed 5 - back to MainPanel");
                    RotPUI.instance().selectMainPanel();
                }
                if (!systemsToAllocate().isEmpty())
                    RotPUI.instance().allocateSystems();

                log("Refreshing Player Views");
                NoticeMessage.resetSubstatus(text("TURN_REFRESHING"));
                validate();
                gal.refreshAllEmpireViews();
                log("Autosaving post-turn");
                log("NEXT TURN PROCESSING TIME: ", str(System.currentTimeMillis()-startMs));
                NoticeMessage.resetSubstatus(text("TURN_SAVING"));
                instance.saveRecentSession(true);

                log("Reselecting main panel");
                RotPUI.instance().mainUI().showDisplayPanel();
                RotPUI.instance().selectMainPanel();
                notifications().clear();
                // ensure Next Turn takes at least a minimum time
                long spentMs = System.currentTimeMillis() - startMs;
                if (spentMs < MINIMUM_NEXT_TURN_TIME) {
                    try { Thread.sleep(MINIMUM_NEXT_TURN_TIME - spentMs);
                    } catch (InterruptedException e) { }
                }
                RotPUI.instance().repaint();
                log("Next Turn - END: ", str(galaxy.currentTurn()));
            }
            catch(Exception e) {
                err("Unexpected error during Next Turn:", e.toString());
                exception(e);
            }
            finally {
                RotPUI.instance().mainUI().restoreMapState();
                if (Rotp.memoryLow())
                    RotPUI.instance().mainUI().showMemoryLowPrompt();
                // handle game over possibility
                if (!session().status().inProgress())
                    RotPUI.instance().selectGameOverPanel();
                performingTurn = false;
            }
        };
    }
    public boolean processNotifications() {
        log("Processing player notifications: ", str(notifications().size()));
        if (haveScoutedSystems())
            session().addTurnNotification(new SystemsScoutedNotification());

        
        if (notifications().isEmpty())
            return false;
        // received a concurrent modification here... iterate over temp array
        List<TurnNotification> notifs = new ArrayList<>(notifications());
        Collections.sort(notifs);
        notifications().clear();

        RotPUI.instance().processNotifications(notifs);
        clearScoutedSystems();
        return true;
    }
    private String nextTurnTitle() {
        return text("MAIN_ADVANCING_YEAR", galaxy().currentTurn()+1);
    }
    public void saveSession(String filename, boolean backup) throws Exception {
        log("Saving game as file: ", filename, "  backup: "+backup);
        GameSession currSession = GameSession.instance();
        File theDir = backup ? new File(backupDir()) : new File(saveDir());
        if (!theDir.exists())
            theDir.mkdirs();
        File saveFile = backup ? backupFileNamed(filename) : saveFileNamed(filename);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(saveFile));
        ZipEntry e = new ZipEntry("GameSession.dat");
        out.putNextEntry(e);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream objOut = null;
        try {
            objOut = new ObjectOutputStream(bos);
            objOut.writeObject(currSession);
            objOut.flush();
            byte[] data = bos.toByteArray();
            out.write(data, 0, data.length);
        }
        finally {
            try {
            bos.close();
            out.close();
            }
            catch(IOException ex) {}
        }
    }
    private void loadPreviousSession(GameSession gs, boolean startUp) {
        stopCurrentGame();
        instance = gs;
        startExecutors();
        RotPUI.instance().mainUI().checkMapInitialized();
        if (!startUp) {
            RotPUI.instance().selectMainPanelLoadGame();
        }
    }
    public String saveDir() {
        return UserPreferences.saveDirectoryPath();
    }
    public String backupDir() {
        return concat(saveDir(),"/",GameSession.BACKUP_DIRECTORY);
    }
    private File saveFileNamed(String fileName) {
        return new File(saveDir(), fileName);
    }
    private File backupFileNamed(String fileName) {
        return new File(backupDir(), fileName);
    }
    private String backupFileName(int num) {
        Empire pl = player();
        String leader = pl.leader().name().replaceAll("\\s", "");
        String race = pl.raceName();
        String gShape = text(options().selectedGalaxyShape()).replaceAll("\\s", "");
        String gSize = text(options().selectedGalaxySize());
        String diff = text(options().selectedGameDifficulty());
        String turn = "T"+pad4.format(num);
        String opp = "vs"+options().selectedNumberOpponents();
        String dash = "-";
        return concat(leader,dash,race,dash,gShape,dash,gSize,dash,diff,dash,opp,dash,turn,SAVEFILE_EXTENSION);
    }
    private void saveRecentSession(boolean endOfTurn) {
        String filename = RECENT_SAVEFILE;
        try {
            saveSession(filename, false);
            if (endOfTurn)
               saveBackupSession(galaxy().currentTurn());
        }
        catch(Exception e) {
            err("Error saving: ", filename, " - ", e.getMessage());
            if (endOfTurn)
                RotPUI.instance().mainUI().showAutosaveFailedPrompt(e.getMessage());
        }
    }
    private void saveBackupSession(int turn) {
        String filename = "nofile";
        try {
            int backupTurns = UserPreferences.backupTurns();
            if (backupTurns > 0) {
                if ((turn == 1) || (turn % backupTurns == 0)) {
                    filename = backupFileName(turn);
                    saveSession(filename, true);
                }
            }
        }
        catch(Exception e) {
            err("Error saving: ", filename, " - ", e.getMessage());
            RotPUI.instance().mainUI().showAutosaveFailedPrompt(e.getMessage());
        }
        
    }
    public boolean hasRecentSession() {
        try {
            InputStream file = new FileInputStream(RECENT_SAVEFILE);
            file.close();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    public void loadRecentSession(boolean startUp) {
        loadSession(saveDir(), RECENT_SAVEFILE, startUp);
    }
    public void loadSession(String dir, String filename, boolean startUp) {
        try {
            log("Loading game from file: ", filename);
            File saveFile = dir.isEmpty() ? new File(filename) : new File(dir, filename);
            GameSession newSession;
            // assume the file is not zipped, load it directly
            try (InputStream file = new FileInputStream(saveFile)) {
                newSession = loadObjectData(file);
            }
            
            // if newSession is null, see if it is zipped
            if (newSession == null) {
                try (ZipFile zipFile = new ZipFile(saveFile)) {
                    ZipEntry ze = zipFile.entries().nextElement();
                    InputStream zis = zipFile.getInputStream(ze);
                    newSession = loadObjectData(zis);
                    if (newSession == null)
                        throw new RuntimeException(text("LOAD_GAME_BAD_VERSION", filename));
                }
            }
            
            GameSession.instance = newSession;
            newSession.validate();
            newSession.validateOnLoadOnly();
            loadPreviousSession(newSession, startUp);
            // do not autosave the current session if that is the file we are trying to reload
            if (!filename.equals(RECENT_SAVEFILE))
                saveRecentSession(false);
        }
        catch(IOException e) {
            throw new RuntimeException(text("LOAD_GAME_BAD_VERSION", filename));
        }
    }
    private GameSession loadObjectData(InputStream is) {
        try {
            GameSession newSession;
            try (InputStream buffer = new BufferedInputStream(is)) {
                ObjectInput input = new ObjectInputStream(buffer);
                newSession = (GameSession) input.readObject();
            }
            return newSession;
        }
        catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
    private void validate() {
        galaxy().validate();
    }
    private void validateOnLoadOnly() {
        GNNExpansionEvent.instance().validate(galaxy());

        // check for invalid colonies with too much waste & negative pop
        for (StarSystem sys: galaxy().starSystems()) {
            if (sys.isColonized())
                sys.colony().validateOnLoad();
        }
        // check for council last-vote init issue
        boolean allVotedOnlyForPlayer = true;
        for (Empire emp: galaxy().empires()) {
            if (emp.lastCouncilVoteEmpId() != 0)
                allVotedOnlyForPlayer = false;
        }

        if (allVotedOnlyForPlayer) {
            for (Empire emp: galaxy().empires())
                emp.lastCouncilVoteEmpId(Empire.NULL_ID);
        }

        Galaxy gal = this.galaxy();
        Empire pl = player();
        
        float minX = gal.width();
        float minY = gal.height();
        float maxX = 0;
        float maxY = 0;

        List<StarSystem> alliedSystems = pl.allColonizedSystems();
        for (StarSystem sys : alliedSystems) {
            minX = min(minX,sys.x());
            maxX = max(maxX,sys.x());
            minY = min(minY,sys.y());
            maxY = max(maxY,sys.y());
        }
        float r = pl.scoutReach(6);
        minX = max(0,minX-r);
        maxX = min(gal.width(), maxX+r);
        minY = max(0,minY-r);
        maxY = min(gal.height(), maxY+r);
        pl.setBounds(minX, maxX, minY, maxY);
        pl.setVisibleShips();
    }
    private static ThreadFactory minThreadFactory() {
        return (Runnable r) -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        };
    }
}
