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
package rotp.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import rotp.Rotp;
import rotp.model.colony.Colony;
import rotp.model.combat.CombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.EspionageMission;
import rotp.model.empires.SabotageMission;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.Transport;
import rotp.model.game.IGameOptions;
import rotp.model.game.MOO1GameOptions;
import rotp.model.planet.PlanetFactory;
import rotp.model.ships.ShipLibrary;
import rotp.model.tech.TechCategory;
import rotp.model.tech.TechLibrary;
import rotp.ui.combat.ShipBattleUI;
import rotp.ui.design.DesignUI;
import rotp.ui.game.HelpUI;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomacyRequestReply;
import rotp.ui.fleets.FleetUI;
import rotp.ui.map.SystemsUI;
import rotp.ui.game.GameOverUI;
import rotp.ui.game.GameSettingsUI;
import rotp.ui.game.GameUI;
import rotp.ui.game.LoadGameUI;
import rotp.ui.game.RaceIntroUI;
import rotp.ui.game.SaveGameUI;
import rotp.ui.game.SetupGalaxyUI;
import rotp.ui.game.SetupRaceUI;
import rotp.ui.game.StartOptionsUI;
import rotp.ui.history.HistoryUI;
import rotp.ui.main.MainUI;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.TurnNotification;
import rotp.ui.planets.GroundBattleUI;
import rotp.ui.planets.PlanetsUI;
import rotp.ui.races.RacesUI;
import rotp.ui.races.SabotageUI;
import rotp.ui.tech.AllocateTechUI;
import rotp.ui.tech.DiplomaticMessageUI;
import rotp.ui.tech.DiscoverTechUI;
import rotp.ui.tech.SelectNewTechUI;
import rotp.ui.util.planets.PlanetImager;
import rotp.util.AnimationManager;
import rotp.util.ImageManager;
import rotp.util.LanguageManager;
import rotp.util.sound.SoundManager;

public class RotPUI extends BasePanel implements ActionListener, KeyListener {
    private static final long serialVersionUID = 1L;
    private static int FPS = 10;
    public static int ANIMATION_TIMER = 100;
    private boolean drawNextTurnNotice = true;
    private static Throwable startupException;
    static {
        // needed for opening ui
        try { UserPreferences.load(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: UserPreferences init "+t.getMessage()); }
        try { TechLibrary.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: TechLibrary init: "+t.getMessage()); }
        try { LanguageManager.current().selectedLanguageName(); }
        catch (Throwable t) { System.out.println("Err: LanguageManager init: "+t.getMessage()); }

        try { SoundManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: SoundManager init: "+t.getMessage()); }

        try { ImageManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: ImageManager init: "+t.getMessage()); }

        try { AnimationManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: AnimationManager init: "+t.getMessage()); }

        try { DialogueManager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: DialogueManager init: "+t.getMessage()); }

        try { ShipLibrary.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: ShipLibrary init: "+t.getMessage()); }

        try { PlanetImager.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: PlanetImager init: "+t.getMessage()); }

        try { PlanetFactory.current(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: PlanetFactory init: "+t.getMessage()); }

        try { UserPreferences.loadAndSave(); }
        catch (Throwable t) { startupException = t; System.out.println("Err: UserPreferences init: "+t.getMessage()); }
    }

    public static boolean useDebugFile = false;
    public static IGameOptions newGameOptions;

    private static final String SETUP_RACE_PANEL = "SetupRace";
    private static final String SETUP_GALAXY_PANEL = "SetupGalaxy";
    private static final String LOAD_PANEL = "Load";
    private static final String SAVE_PANEL = "Save";
    private static final String INTRO_PANEL = "Intro";
    private static final String MAIN_PANEL = "Main";
    private static final String GAME_PANEL = "Game";
    private static final String DESIGN_PANEL = "Design";
    private static final String FLEET_PANEL = "Fleet";
    private static final String SYSTEMS_PANEL = "Systems";
    private static final String RACES_PANEL = "Races";
    private static final String PLANETS_PANEL = "Planets";
    private static final String TECH_PANEL = "Tech";
    private static final String SELECT_NEW_TECH_PANEL = "NewTech";
    private static final String DISCOVER_TECH_PANEL = "DiscoverTech";
    private static final String DIPLOMATIC_MESSAGE_PANEL = "DiplomaticMessage";
    private static final String SHIP_BATTLE_PANEL = "ShipBattle";
    private static final String GROUND_BATTLE_PANEL = "GroundBattle";
    private static final String SABOTAGE_PANEL = "Sabotage";
    private static final String GNN_PANEL = "GNN";
    private static final String COUNCIL_PANEL = "GalacticCouncil";
    private static final String GAME_OVER_PANEL = "GameOver,Man,GameOver";
    private static final String ERROR_PANEL = "Error";
    private static final String DIALOG_PANEL = "Dialog";

    private static final RotPUI instance = new RotPUI();

    private static PrintWriter debugFile = null;

    public static void fps(int fps) {
        // bound arg between 10 & 60
        int actualFPS = Math.min(60, Math.max(10,fps));
        if (FPS == actualFPS)
            return;

        FPS = actualFPS;
        ANIMATION_TIMER = 1000/FPS;
        instance.resetTimer();
    }
    public static int scaledSize(int i) {
        if (i < 1)
            return (int) Math.ceil(Rotp.resizeAmt()*i);
        else if (i > 1)
            return (int) Math.floor(Rotp.resizeAmt()*i);
        else
            return i;
    }
    public static int unscaledSize(int i) {
        return (int) Math.max(0, Math.ceil(i/Rotp.resizeAmt()));
    }
    public static PrintWriter debugFile() {
        if (!useDebugFile)
            return null;

        if (debugFile == null) {
            try {
                FileOutputStream fout = new FileOutputStream(new File("rotp_log.txt"));
                debugFile = new PrintWriter(fout, true);
            }
            catch (FileNotFoundException e) {
                System.err.println("RotpUI.static<> -- Unable to open debug file:  FileNotFoundException: " + e);
            }
        }
        return debugFile;
    }

    private final GameUI gameUI = new GameUI();
    private final LoadGameUI loadGameUI = new LoadGameUI();
    private final SaveGameUI saveGameUI = new SaveGameUI();
    private final SetupRaceUI setupRaceUI = new SetupRaceUI();
    private final SetupGalaxyUI setupGalaxyUI = new SetupGalaxyUI();
    private final RaceIntroUI raceIntroUI = new RaceIntroUI();
    private MainUI mainUI;
    private final DesignUI designUI = new DesignUI();
    private final FleetUI fleetUI = new FleetUI();
    private final SystemsUI systemsUI = new SystemsUI();
    private final RacesUI racesUI = new RacesUI();
    private final PlanetsUI planetsUI = new PlanetsUI();
    private final AllocateTechUI allocateTechUI = new AllocateTechUI();
    private final SelectNewTechUI selectNewTechUI = new SelectNewTechUI();
    private final DiscoverTechUI discoverTechUI = new DiscoverTechUI();
    private final ShipBattleUI shipBattleUI = new ShipBattleUI();
    private final GroundBattleUI groundBattleUI = new GroundBattleUI();
    private final SabotageUI sabotageUI = new SabotageUI();
    private final HistoryUI historyUI = new HistoryUI();
    private final GNNUI gnnUI = new GNNUI();
    private final DiplomaticMessageUI diplomaticMessageUI = new DiplomaticMessageUI();
    private final GalacticCouncilUI galacticCouncilUI = new GalacticCouncilUI();
    private final GameOverUI gameOverUI = new GameOverUI();
    private final ErrorUI errorUI = new ErrorUI();
    private final HelpUI helpUI = new HelpUI();
    private final StartOptionsUI startOptionsUI = new StartOptionsUI();
    private final GameSettingsUI gameSettingsUI = new GameSettingsUI();
    private final LargeDialogPane dialogPane = new LargeDialogPane();

    private final CardLayout layout = new CardLayout();
    private String currentPane = GAME_PANEL;
    private BasePanel selectedPanel;

    private Timer timer;
    private int animationCount = 0;
    public MainUI mainUI() {
        if (mainUI == null)
            mainUI = new MainUI();
        return mainUI;
    }
    public RaceIntroUI raceIntroUI()  { return raceIntroUI; }
    public AllocateTechUI techUI()    { return allocateTechUI; }
    public RacesUI racesUI()          { return racesUI; }
    @Override
    public int animationCount()     { return animationCount; }

    public RotPUI() {
        timer = new Timer(ANIMATION_TIMER, this);
        init();
    }
    public void processNotifications(List<TurnNotification> notifications) {
        for (TurnNotification tn: notifications)
            processNotification(tn);
    }
    public void processNotification(TurnNotification tn) {
        try {
            drawNextTurnNotice = false;
            tn.notifyPlayer();
        } finally {
            drawNextTurnNotice = true;
        }
    }

    private void resetTimer() {
        if (timer != null)
            timer.stop();
        timer = new Timer(ANIMATION_TIMER, this);
        timer.start();
    }
    private void init() {
        initModel();
        addKeyListener(this);
        if (startupException != null)
            selectErrorPanel(startupException);
        else
            selectCurrentPanel();

        timer.start();
        //toggleAnimations();
        repaint();
    }
    public static IGameOptions newOptions() {
        if (newGameOptions == null)
            createNewOptions();
        return newGameOptions;
    }
    public static void createNewOptions()               { newGameOptions = new MOO1GameOptions(); }
    public static void clearNewOptions()                { newGameOptions = null; }

    public void toggleAnimations() {
        if (playAnimations())
            timer.start();
        else
            timer.stop();
    }
    public static RotPUI instance()                  { return instance; }
    public static HelpUI helpUI()                    { return instance.helpUI; }
    public static StartOptionsUI startOptionsUI()    { return instance.startOptionsUI; }
    public static GameSettingsUI gameSettingsUI()    { return instance.gameSettingsUI; }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (drawNextTurnNotice && session().performingTurn()) {
            drawNotice(g, 28, -s100);
        }
        requestFocusInWindow();
    }
    public void repaintNotice() {
        int w0 = scaled(500);
        int h0 = s100;
        int x0 = (getWidth()-w0)/2;
        int y0 = (getHeight()-h0)/2;
        repaint(x0,y0,w0,h0);
    }
    public void selectCurrentPanel()   { selectPanel(currentPane, selectedPanel); }

    // PLAYER-TRIGGERED ACTIONS
    public void selectSetupRacePanel() { setupRaceUI.init(); selectPanel(SETUP_RACE_PANEL, setupRaceUI);  }
    public void selectSetupGalaxyPanel() { setupGalaxyUI.init(); selectPanel(SETUP_GALAXY_PANEL, setupGalaxyUI);  }
    public void selectLoadGamePanel()  { loadGameUI.init(); selectPanel(LOAD_PANEL, loadGameUI);  }
    public void selectSaveGamePanel()  { saveGameUI.init(); selectPanel(SAVE_PANEL, saveGameUI);  }
    public void selectIntroPanel()     {
        mainUI.init(false);
        selectPanel(MAIN_PANEL, mainUI());
        enableGlassPane(raceIntroUI);
        repaint();
    }
    public void selectMainPanel()      { selectMainPanel(false); }
    public void selectMainPanel(boolean pauseNextTurn)      {
        disableGlassPane();
        if (!session().status().inProgress()) {
            selectGameOverPanel();
            return;
        }
        mainUI.init(pauseNextTurn);
        selectPanel(MAIN_PANEL, mainUI());
        repaint();
    }
    public void selectMainPanelLoadGame() {
        disableGlassPane();
        if (!session().status().inProgress()) {
            selectGameOverPanel();
            return;
        }
        mainUI.clearOverlay();
        mainUI.init(false);
        mainUI.showDisplayPanel();
        selectPanel(MAIN_PANEL, mainUI());
        repaint();
    }
    public void selectMainPanelNewGame()      {
        disableGlassPane();
        mainUI.clearOverlay();
        mainUI.init(true);
        mainUI.showDisplayPanel();
        selectPanel(MAIN_PANEL, mainUI());
        repaint();
    }
    public void selectGamePanel()      {
        gameUI.init();
        if (!UserPreferences.windowed())
            selectDialogPanel(GAME_PANEL, gameUI);
        else
            selectPanel(GAME_PANEL, gameUI);
    }
    public void selectDesignPanel()    { designUI.init(); selectPanel(DESIGN_PANEL, designUI); }
    public void selectFleetPanel()     { fleetUI.init(); selectPanel(FLEET_PANEL, fleetUI); }
    public void selectSystemsPanel()   { systemsUI.init(); selectPanel(SYSTEMS_PANEL, systemsUI); }
    public void selectRacesPanel()     { racesUI.init(); selectPanel(RACES_PANEL, racesUI); }
    public void selectPlanetsPanel()   { planetsUI.init(); selectPanel(PLANETS_PANEL, planetsUI); }
    public void selectTechPanel()      { allocateTechUI.init(); selectPanel(TECH_PANEL, allocateTechUI); }
    public void selectCouncilPanel()   {
        session().pauseNextTurnProcessing("Show Council");
        galacticCouncilUI.init();
        if (!UserPreferences.windowed())
            selectDialogPanel(COUNCIL_PANEL, galacticCouncilUI);
        else
            selectPanel(COUNCIL_PANEL, galacticCouncilUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectGameOverPanel()  {
        gameOverUI.init();
        if (!UserPreferences.windowed())
            selectDialogPanel(GAME_OVER_PANEL, gameOverUI);
        else
            selectPanel(GAME_OVER_PANEL, gameOverUI);
    }
    public void selectErrorPanel(Throwable e)  {
        // ignore low-level mp3 errors from the Java FX library
        for (StackTraceElement line : e.getStackTrace()) {
            if (line.toString().contains("com.sun.media.jfxmediaimpl.NativeMediaPlayer.sendWarning")) {
                err("IGNORED JAVA MEDIA WARNING: ");
                e.printStackTrace();
                return;
            }
            if (line.toString().contains("com.sun.media.sound.DirectAudioDevice")) {
                err("IGNORED JAVA MEDIA WARNING: ");
                e.printStackTrace();
                SoundManager.loadSounds();
                return;
            }
        }
        //e.printStackTrace();
        errorUI.init(e);
        selectPanel(ERROR_PANEL, errorUI);
    }
    public void selectGNNPanel(String title, String id, List<Empire> empires) {
        session().pauseNextTurnProcessing("Show GNN");
        gnnUI.init(title, id, empires);
        if (!UserPreferences.windowed())
            selectDialogPanel(GNN_PANEL, gnnUI);
        else
            selectPanel(GNN_PANEL, gnnUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectSabotagePanel(SabotageMission m, int sysId) {
        session().pauseNextTurnProcessing("Show Sabotage");
        sabotageUI.init(m, sysId);
        selectPanel(SABOTAGE_PANEL, sabotageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectHistoryPanel(int empId, boolean showAll) {
        historyUI.init(empId, showAll);
        enableGlassPane(historyUI);
    }
    public void selectGroundBattlePanel(Colony c, Transport tr) {
        session().pauseNextTurnProcessing("Show Ground Battle");
        groundBattleUI.init(c, tr);
        if (!UserPreferences.windowed())
            selectDialogPanel(GROUND_BATTLE_PANEL, groundBattleUI);
        else
            selectPanel(GROUND_BATTLE_PANEL, groundBattleUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void showBombardmentNotice(int sysId, ShipFleet fl) {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Bombard Notice");
            mainUI().showBombardmentNotice(sysId, fl);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void promptForBombardment(int sysId, ShipFleet fl) {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Bombard Prompt");
            mainUI().showBombardmentPrompt(sysId, fl);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void promptForShipCombat(CombatManager mgr) {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Ship Combat Prompt");
            mainUI().showShipCombatPrompt(mgr);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void selectShipBattlePanel(CombatManager mgr, int combatFlag) {
        boolean showBattle = shipBattleUI.init(mgr, combatFlag);
        if (showBattle)
            selectPanel(SHIP_BATTLE_PANEL, shipBattleUI);
        else
            session().resumeNextTurnProcessing();
    }
    public void promptForColonization(int sysId, ShipFleet fl) {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Colonize Prompt");
            mainUI().showColonizationPrompt(sysId, fl);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void selectSelectNewTechPanel(TechCategory cat) {
        session().pauseNextTurnProcessing("Show Select Tech");
        selectNewTechUI.category(cat);
        if (!UserPreferences.windowed())
            selectDialogPanel(SELECT_NEW_TECH_PANEL, selectNewTechUI);
        else
            selectPanel(SELECT_NEW_TECH_PANEL, selectNewTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectPlunderShipTechPanel(String techId, int empId) {
        session().pauseNextTurnProcessing("Show Plunder Tech");
        discoverTechUI.plunderShipTech(techId, empId);
        if (!UserPreferences.windowed())
            selectDialogPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        else
            selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectPlunderTechPanel(String techId, int sysId, int empId) {
        session().pauseNextTurnProcessing("Show Plunder Tech");
        discoverTechUI.plunderTech(techId, sysId, empId);
        if (!UserPreferences.windowed())
            selectDialogPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        else
            selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectDiscoverTechPanel(String techId) {
        session().pauseNextTurnProcessing("Show Discover Tech");
        discoverTechUI.discoverTech(techId);
        if (!UserPreferences.windowed())
            selectDialogPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        else
            selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectTradeTechPanel(String techId, int empId) {
        session().pauseNextTurnProcessing("Show Trade Tech");
        discoverTechUI.tradeTech(techId, empId);
        if (!UserPreferences.windowed())
            selectDialogPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        else
            selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectStealTechPanel(EspionageMission mission, int empId) {
        discoverTechUI.stealTech(mission, empId);
        if (!UserPreferences.windowed())
            selectDialogPanel(DISCOVER_TECH_PANEL, discoverTechUI);
        else
            selectPanel(DISCOVER_TECH_PANEL, discoverTechUI);
    }
    public void selectEspionageMissionPanel(EspionageMission mission, int empId) {
       try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Espionage");
            log("==MAIN UI==   espionage mission");
            mainUI().showEspionageMission(mission, empId);
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }

    public void selectDiplomaticMessagePanel(DiplomaticNotification notif) {
        session().pauseNextTurnProcessing("Show Diplomatic Message");
        log("==MAIN UI==   selectDiplomaticMessagePanel");
        diplomaticMessageUI.init(notif);
        if (!UserPreferences.windowed())
            selectDialogPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        else
            selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void selectDiplomaticDialoguePanel(DiplomaticNotification notif) {
        log("==MAIN UI==   selectDiplomaticDialoguePanel");
        diplomaticMessageUI.init(notif);
        diplomaticMessageUI.endFade();
        if (!UserPreferences.windowed())
            selectDialogPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        else
            selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
    }
    public void selectDiplomaticReplyPanel(DiplomacyRequestReply reply) {
        log("==MAIN UI==   selectDiplomaticReplyPanel");
        diplomaticMessageUI.initReply(reply);
        diplomaticMessageUI.endFade();
        if (!UserPreferences.windowed())
            selectDialogPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        else
            selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
    }
    public void selectDiplomaticReplyModalPanel(DiplomacyRequestReply reply) {
        session().pauseNextTurnProcessing("Show Diplomatic Reply");
        log("==MAIN UI==   selectDiplomaticReplyModalPanel");
        diplomaticMessageUI.initReply(reply);
        diplomaticMessageUI.endFade();
        if (!UserPreferences.windowed())
            selectDialogPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        else
            selectPanel(DIPLOMATIC_MESSAGE_PANEL, diplomaticMessageUI);
        session().waitUntilNextTurnCanProceed();
    }
    public void showTransportAlert(String title, String subtitle, String text) {  }
    public void showSpyAlert(String title, String subtitle, String text) {  }
    public void showRandomEventAlert(String title, String subtitle, String text, ImageIcon splash) { }
    public void allocateSystems() {
        try {
            drawNextTurnNotice = false;
            session().pauseNextTurnProcessing("Show Allocate Systems");
            log("==MAIN UI==   allocate systems");
            mainUI().allocateSystems(session().systemsToAllocate());
            selectMainPanel();
            session().waitUntilNextTurnCanProceed();
        } finally {
            drawNextTurnNotice = true;
        }
    }
    public void showSystemsScouted() {
        session().pauseNextTurnProcessing("Show Systems Scouted");
        log("==MAIN UI==   show systems scouted");
        mainUI().showSystemsScouted(session().systemsScouted());
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    public void showSpyReport() {
        session().pauseNextTurnProcessing("Show SpyReport");
        log("==MAIN UI==   show spy report");
        mainUI().showSpyReport();
        selectMainPanel();
        session().waitUntilNextTurnCanProceed();
    }
    private void initModel() {
        setFocusTraversalKeysEnabled(false);
        setBackground(Color.CYAN);
        setLayout(layout);

        if (!UserPreferences.windowed())
            dialogPane.addToLayout(gameUI, GAME_PANEL);
        else
            add(gameUI, GAME_PANEL);
                
        add(setupRaceUI, SETUP_RACE_PANEL);
        add(setupGalaxyUI, SETUP_GALAXY_PANEL);
        add(loadGameUI, LOAD_PANEL);
        add(saveGameUI, SAVE_PANEL);
        add(raceIntroUI, INTRO_PANEL);
        add(mainUI(), MAIN_PANEL);
        add(designUI, DESIGN_PANEL);
        add(fleetUI, FLEET_PANEL);
        add(systemsUI, SYSTEMS_PANEL);
        add(racesUI, RACES_PANEL);
        add(planetsUI, PLANETS_PANEL);
        add(allocateTechUI, TECH_PANEL);
        add(sabotageUI, SABOTAGE_PANEL);
        add(shipBattleUI, SHIP_BATTLE_PANEL);
        add(errorUI, ERROR_PANEL);
        add(dialogPane, DIALOG_PANEL);

        if (!UserPreferences.windowed()) {
            dialogPane.addToLayout(diplomaticMessageUI, DIPLOMATIC_MESSAGE_PANEL);
            dialogPane.addToLayout(selectNewTechUI, SELECT_NEW_TECH_PANEL);
            dialogPane.addToLayout(discoverTechUI, DISCOVER_TECH_PANEL);
            dialogPane.addToLayout(groundBattleUI, GROUND_BATTLE_PANEL);
            dialogPane.addToLayout(gnnUI, GNN_PANEL);
            dialogPane.addToLayout(galacticCouncilUI, COUNCIL_PANEL);
            dialogPane.addToLayout(gameOverUI, GAME_OVER_PANEL);
        }
        else {
            add(diplomaticMessageUI, DIPLOMATIC_MESSAGE_PANEL);
            add(selectNewTechUI, SELECT_NEW_TECH_PANEL);
            add(discoverTechUI, DISCOVER_TECH_PANEL);
            add(groundBattleUI, GROUND_BATTLE_PANEL);
            add(gnnUI, GNN_PANEL);
            add(galacticCouncilUI, COUNCIL_PANEL);
            add(gameOverUI, GAME_OVER_PANEL);
        }
        selectGamePanel();
    }
    private void selectDialogPanel(String panelName, BasePanel panel)   {
        currentPane = panelName;
        selectedPanel = panel;
        selectedPanel.playAmbience();
        dialogPane.selectPanel(panelName, panel);
        layout.show(this, DIALOG_PANEL);
    }
    private void selectPanel(String panelName, BasePanel panel)   {
        currentPane = panelName;
        selectedPanel = panel;
        selectedPanel.playAmbience();
        log("showing panel: ", panelName);
        layout.show(this, panelName);
    }
    @Override
    public void enableGlassPane(BasePanel panel)   {
        super.enableGlassPane(panel);
        panel.playAmbience();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        animationCount++;
        animate();
    }
    @Override
    public void animate() {
        try {
            AnimationManager.reclaimImages();
            if (playAnimations()) {
                if (glassPane() != null)
                    glassPane().animate();
                if (selectedPanel != null)
                    selectedPanel.animate();
            }
        }
        catch (Exception e) {
            // we have to catch all errors or else the
            // animation timer stops completely
            e.printStackTrace();
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyPressed(e);
        else if (selectedPanel != null)
            selectedPanel.keyPressed(e);
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyReleased(e);
        else if (selectedPanel != null)
            selectedPanel.keyReleased(e);
    }
    @Override
    public void keyTyped(KeyEvent e) {
        if (glassPane() != null)
            glassPane().keyTyped(e);
        else if (selectedPanel != null)
            selectedPanel.keyTyped(e);
    }
    public class LargeDialogPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        private final CardLayout dialogLayout = new CardLayout();
        private final BasePanel dialogHolder = new BasePanel();
        public LargeDialogPane() {
            initModel();
        }
        @Override
        public boolean hasStarBackground()   { return true; }
        private void initModel() {
            setOpaque(true);
            setBackground(Color.black);
            dialogHolder.setLayout(dialogLayout);
            
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            int border = s10;
            int w = size.width-border-border;
            int h = size.height-border-border;
            
            BasePanel barTop    = new BasePanel();
            BasePanel barBottom = new BasePanel();
            BasePanel barLeft   = new BasePanel();
            BasePanel barRight  = new BasePanel();
            barTop.setOpaque(false);
            barBottom.setOpaque(false);
            barLeft.setOpaque(false);
            barRight.setOpaque(false);
            
            int barW = 0;
            int barH = 0;
            boolean sideBars = w > (h*8/5);
        
            if (sideBars) {
                barW = (w-(h*8/5))/2;
                barH = h;
                barTop.setPreferredSize(new Dimension(w+border+border, border));
                barBottom.setPreferredSize(new Dimension(w+border+border, border));
                barLeft.setPreferredSize(new Dimension(barW+border, barH));
                barRight.setPreferredSize(new Dimension(barW+border, barH));
            }
            else {
                barH = (h-(w*5/8))/2;
                barW = w;
                barTop.setPreferredSize(new Dimension(w+border+border, barH));
                barBottom.setPreferredSize(new Dimension(w+border+border, barH));
                barLeft.setPreferredSize(new Dimension(border, barH));
                barRight.setPreferredSize(new Dimension(border, barH));
            }
            
            setLayout(new BorderLayout());
            add(barTop, BorderLayout.NORTH);
            add(barBottom, BorderLayout.SOUTH);
            add(barLeft, BorderLayout.WEST);
            add(barRight, BorderLayout.EAST);
            add(dialogHolder, BorderLayout.CENTER);
        }
        public void addToLayout(BasePanel panel, String key) {
            dialogHolder.add(panel, key);
        }
        public void selectPanel(String panelName, BasePanel panel)   {
            log("showing dialog panel: ", panelName);
            dialogLayout.show(dialogHolder, panelName);
        }
    }
    public class SideBarPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        public SideBarPane() {
            setOpaque(false);
            setBackground(Color.black);
        }

    }
}
