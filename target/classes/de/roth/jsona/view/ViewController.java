package de.roth.jsona.view;

import de.roth.jsona.MainFX;
import de.roth.jsona.api.youtube.YoutubeVideoStreamURL;
import de.roth.jsona.config.Config;
import de.roth.jsona.config.Global;
import de.roth.jsona.information.Link;
import de.roth.jsona.logic.LogicInterfaceFX;
import de.roth.jsona.model.*;
import de.roth.jsona.model.MusicListItem.PlaybackStatus;
import de.roth.jsona.theme.ThemeUtils;
import de.roth.jsona.util.Logger;
import de.roth.jsona.util.TimeFormatter;
import de.roth.jsona.view.draghandler.ListItemDragHandler;
import de.roth.jsona.view.util.AlignmentUtil;
import de.roth.jsona.view.util.BrowserUtil;
import de.roth.jsona.view.util.DialogUtil;
import de.roth.jsona.view.util.TabUtil;
import de.roth.jsona.vlc.mediaplayer.PlayBackMode;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static de.roth.jsona.api.youtube.YoutubeVideoStreamURL.StreamType;

public class ViewController implements Initializable, ViewInterface {

    // JavaFX UI-Components
    @FXML
    private AnchorPane applicationContainer, searchContent, controlPanel, outerContainer;

    @FXML
    private Slider volumeSlider, durationSlider;

    @FXML
    private BorderPane informationContainer;

    @FXML
    private SplitPane musicListsAnchorContainer;

    @FXML
    private ProgressBar volumeProgress, durationProgress;
    boolean blockDurationProgress;

    @FXML
    private Label volumeLabel, durationLabel, artistLabel, titleLabel, artistBio;

    @FXML
    private TabPane musicTabs, playlistTabs;

    @FXML
    private Tab searchTab;

    @FXML
    private FlowPane topTracks;

    @FXML
    private Button playButton, nextButton, prevButton, modeButton;

    @FXML
    private Hyperlink removePlaylistButton;

    @FXML
    private ImageView playButtonImage, pauseButtonImage, nextButtonImage, prevButtonImage, modeButtonImage, artistImage, addPlaylistImage, equalizerIcon, resizer, closeWindowIcon, maximizeWindowIcon, minimizeWindowIcon;

    @FXML
    private Image playImage, pauseImage, maximizeImage, reMaximizeImage, modeNormalImage, modeShuffleImage, modeRepeatImage;

    @FXML
    private AnchorPane imageContainer;

    @FXML
    private TextField searchText;

    @FXML
    private ListView<MusicListItem> searchResultsListView;

    private Stage stage;
    private Scene scene;
    private ArrayList<ListView<MusicListItem>> playListViews;
    private HashMap<String, Tab> musicFolderTabs;
    private HashMap<String, ListView<MusicListItem>> musicFolderListViews;
    private HashMap<String, ProgressIndicator> musicFolderListLoadingViews;
    private Stage equalizerStage;

    // Dragging data
    private static final DataFormat DATA_FORMAT_LIST = new DataFormat("java.util.List");

    // Tmp
    private int searchResultCounter = -1;

    public ViewController(Stage stage) {
        this.stage = stage;
    }

    // Moving
    private double xOffset = 0;
    private double yOffset = 0;

    // Resizing
    private static double initX = -1;
    private static double initY = -1;
    private static double newX;
    private static double newY;
    private static Rectangle stageDimension = new Rectangle();

    // Language
    ResourceBundle languageBundle;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // load language bundle
        try {
            InputStream inputStream = getClass().getClassLoader().getResource(Global.LANGUAGE_FOLDER + "/" + Config.getInstance().LANGUAGE_FILE).openStream();
            languageBundle = new PropertyResourceBundle(inputStream);
        } catch (IOException e) {
            Logger.get().warn("Could not load language file");
            e.printStackTrace();
        }

        // os window decoration
        if (Config.getInstance().WINDOW_OS_DECORATION == true) {

            // remove window controlling icons
            Node[] items = {closeWindowIcon, maximizeWindowIcon, minimizeWindowIcon, resizer};

            for (Node n : items) {
                n.setManaged(false);
                n.setVisible(false);
            }

            outerContainer.setTopAnchor(applicationContainer, (double) 0);
            outerContainer.setRightAnchor(applicationContainer, (double) 0);
            outerContainer.setBottomAnchor(applicationContainer, (double) 0);
            outerContainer.setLeftAnchor(applicationContainer, (double) 0);
        }

        // Initialize view
        this.playListViews = new ArrayList<ListView<MusicListItem>>();
        this.musicFolderListViews = new HashMap<String, ListView<MusicListItem>>();
        this.musicFolderTabs = new HashMap<String, Tab>();
        this.musicFolderListLoadingViews = new HashMap<String, ProgressIndicator>();

        this.outerContainer.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        this.outerContainer.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);

                if (maximizeWindowIcon.getImage().equals(maximizeImage)) {
                    return;
                }
                maximizeWindowIcon.setImage(maximizeImage);
            }
        });
        this.outerContainer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    if (mouseEvent.getClickCount() == 2) {
                        maximizeWindow();
                    }
                }
            }
        });

        // Clear dummy hyperlinks
        topTracks.getChildren().clear();
    }

    private void minimizeWindow() {
        stage.setIconified(true);
    }

    private boolean windowIsMaximized() {
        ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
        Rectangle2D bounds = screens.get(0).getVisualBounds();

        if (stage.getX() == bounds.getMinX() && stage.getY() == bounds.getMinY() && stage.getWidth() == bounds.getWidth() && stage.getHeight() == bounds.getHeight()) {
            return true;
        }

        return false;
    }

    private Rectangle2D preMaximizedPosition = null;

    private void maximizeWindow() {
        if (windowIsMaximized()) {
            stage.setX(preMaximizedPosition.getMinX());
            stage.setY(preMaximizedPosition.getMinY());
            stage.setWidth(preMaximizedPosition.getWidth());
            stage.setHeight(preMaximizedPosition.getHeight());
            this.maximizeWindowIcon.setImage(maximizeImage);

        } else {
            this.maximizeWindowIcon.setImage(reMaximizeImage);
            preMaximizedPosition = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));

            Rectangle2D bounds = screens.get(0).getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());

            setStageWidth(stage, stage.getWidth());
            setStageHeight(stage, stage.getHeight());

            stageDimension.setWidth(stage.getWidth());
            stageDimension.setHeight(stage.getHeight());

            scene.getRoot().setClip(stageDimension);
        }
    }

    private void setVolumeFX(int value, boolean updateItself) {
        // Volume
        if (updateItself) {
            volumeSlider.valueProperty().set(value);
        }
        volumeProgress.setProgress(value / 100d);
        volumeLabel.setText(String.valueOf(value));
    }

    private void setDurationFX(long ms, boolean updateItself) {
        if (updateItself) {
            durationSlider.setValue(ms);
        }
        durationProgress.setProgress(ms / durationSlider.getMax());
        durationLabel.setText(TimeFormatter.formatMilliseconds(ms));
    }

    private void setPlayBackModeFX(PlayBackMode mode) {
        switch (mode) {
            case NORMAL:
                modeButtonImage.setImage(modeNormalImage);
                break;
            case SHUFFLE:
                modeButtonImage.setImage(modeShuffleImage);
                break;
            case REPEAT:
                modeButtonImage.setImage(modeRepeatImage);
                break;
        }
    }

    public Slider getDurationSlider() {
        return durationSlider;
    }

    public Stage getStage() {
        return this.stage;
    }

    private void setDurationLength(long ms) {
        durationSlider.setMax(ms);
    }

    public void hide() {
        stage.hide();
    }

    public void toggleView() {
        Platform.runLater(new Runnable() {
            public void run() {
                if (!stage.isFocused()) {
                    stage.setIconified(false);
                    stage.toFront();
                } else {
                    if (!stage.isIconified()) {
                        stage.setIconified(true);
                    }
                }
            }
        });
    }

    public void removeItem(final MusicListItem item) {
        Platform.runLater(new Runnable() {
            public void run() {

                // Remove from playLists
                for (ListView<MusicListItem> listView : playListViews) {
                    listView.getItems().remove(item);
                }

                // Remove from musicLists
                musicFolderListViews.get(item.toString()).getItems().remove(item);

                // Remove from new
                musicFolderListViews.get(Global.NEW_FOLDER_NAME).getItems().remove(item);

                // Remove from searchList
                searchResultsListView.getItems().remove(item);
            }
        });
    }

    boolean setStageWidth(Stage stage, double width) {
        if (width >= stage.getMinWidth()) {
            stage.setWidth(width);
            initX = newX;
            return true;
        }
        return false;
    }

    boolean setStageHeight(Stage stage, double height) {
        if (height >= stage.getMinHeight()) {
            stage.setHeight(height);
            initY = newY;
            return true;
        }
        return false;
    }

    public void init(final LogicInterfaceFX logic, String theme) {
        // Setup theme
        ClassLoader cl = getClass().getClassLoader();

        // Images
        this.playImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/play.png"));
        this.pauseImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/pause.png"));
        this.playButtonImage.setImage(playImage);
        this.nextButtonImage.setImage(new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/next.png")));
        this.prevButtonImage.setImage(new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/prev.png")));
        this.artistImage.setImage(new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/icon.png")));
        this.maximizeImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/maximize_window.png"));
        this.reMaximizeImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/remaximize_window.png"));
        this.modeNormalImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/mode_normal.png"));
        this.modeShuffleImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/mode_shuffle.png"));
        this.modeRepeatImage = new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/mode_repeat.png"));
        this.modeButtonImage.setImage(modeNormalImage);

        if (logic.equalizer_available()) {
            this.equalizerIcon.setImage(new Image(cl.getResourceAsStream(ThemeUtils.getThemePath() + "/equalizer.png")));
        } else {
            this.equalizerIcon.setImage(null);
            this.equalizerIcon.setDisable(true);
        }

        // Font
        if (new File(ThemeUtils.getThemePath() + "/jsona.otf").exists()) {
            Font.loadFont(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/jsona.otf").toExternalForm(), 10);
        }

        this.maximizeWindowIcon.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                maximizeWindow();
            }
        });

        this.minimizeWindowIcon.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                minimizeWindow();
            }
        });

        // Application keys
        getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        musicTabs.getSelectionModel().select(searchTab);

                        // Does not has any effects, expect from if search tab
                        // is already selected
                        searchText.requestFocus();
                    }
                });
            }
        });
        searchContent.visibleProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> newVal, Boolean oldVal, Boolean arg2) {
                // If the pane from the search tab gets visible, focus the
                // search text field
                if (newVal.getValue()) {
                    searchText.requestFocus();
                }
            }
        });

        // Buttons
        playButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                logic.event_player_play_pause();
            }
        });

        nextButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                durationSlider.setValue(0);
                durationProgress.setProgress(0);
                ListItemManager.getInstance().next(logic);
            }
        });

        prevButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                durationSlider.setValue(0);
                durationProgress.setProgress(0);
                ListItemManager.getInstance().prev(logic);
            }
        });

        removePlaylistButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                final Stage dialog = DialogUtil.createDialog(stage, getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_confirm_dialog.fxml"), false);
                Pane root = (Pane) dialog.getScene().getRoot();

                Button ok = (Button) root.lookup("#okButton");
                Button abort = (Button) root.lookup("#abortButton");
                Label message = (Label) root.lookup("#confirmMessage");
                message.setText("Do you really want to delete the playlist '" + ((Label) playlistTabs.getSelectionModel().getSelectedItem().getGraphic()).getText() + "'?");

                ok.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        // Delete the tab and playlist
                        Tab t = playlistTabs.getSelectionModel().getSelectedItem();
                        String atomicId = t.getId();
                        playlistTabs.getTabs().remove(t);
                        logic.event_playlist_remove(atomicId);

                        dialog.hide();
                    }
                });

                abort.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        dialog.hide();
                    }
                });

                dialog.show();
            }
        });
        removePlaylistButton.setTooltip(new Tooltip("Remove selected playlist."));

        // Mode Button
        modeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                switch (ListItemManager.getInstance().getPlayBackMode()) {
                    case NORMAL:
                        setPlaybackMode(PlayBackMode.SHUFFLE);
                        logic.event_playbackmode_shuffle();
                        break;
                    case SHUFFLE:
                        setPlaybackMode(PlayBackMode.REPEAT);
                        logic.event_playbackmode_repeat();
                        break;
                    case REPEAT:
                        setPlaybackMode(PlayBackMode.NORMAL);
                        ;
                        logic.event_playbackmode_normal();
                        break;
                }
            }
        });

        // Slider
        setVolumeFX(Config.getInstance().VOLUME, true);
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
                setVolumeFX(newValue.intValue(), false);
                logic.event_player_volume(newValue.intValue(), oldValue.intValue());
            }
        });
        volumeSlider.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent arg0) {
                if (arg0.getDeltaY() > 0) {
                    logic.action_player_volume_up(Config.getInstance().VOLUME_SCROLL_UP_DOWN_AMOUNT);
                } else {
                    logic.action_player_volume_down(Config.getInstance().VOLUME_SCROLL_UP_DOWN_AMOUNT);
                }
            }
        });

        durationSlider.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent m) {
                blockDurationProgress = true;
            }

            ;
        });
        durationSlider.addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent m) {
                blockDurationProgress = false;
                logic.event_player_play_skipto(durationSlider.getValue());
            }

            ;
        });
        durationSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
                setDurationFX(newValue.longValue(), false);
            }
        });
        durationSlider.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent arg0) {
                double newTime;

                switch (arg0.getCode()) {
                    case RIGHT:
                        newTime = durationSlider.getValue() + Config.getInstance().DURATION_ARROW_KEYS_SKIP_TIME * 1000;
                        if (newTime > durationSlider.getMax()) {
                            newTime = durationSlider.getMax();
                        }
                        logic.event_player_play_skipto(newTime);
                        break;
                    case LEFT:
                        newTime = durationSlider.getValue() - Config.getInstance().DURATION_ARROW_KEYS_SKIP_TIME * 1000;
                        if (newTime < 0) {
                            newTime = 0;
                        }
                        logic.event_player_play_skipto(newTime);
                        break;
                    default:
                        break;
                }

            }
        });
        durationSlider.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent arg0) {
                double newTime;
                if (arg0.getDeltaY() > 0) {
                    newTime = durationSlider.getValue() + Config.getInstance().DURATION_SCROLL_SKIP_TIME * 1000;
                    if (newTime > durationSlider.getMax()) {
                        newTime = durationSlider.getMax();
                    }
                    logic.event_player_play_skipto(newTime);
                } else {
                    newTime = durationSlider.getValue() - Config.getInstance().DURATION_SCROLL_SKIP_TIME * 1000;
                    if (newTime < 0) {
                        newTime = 0;
                    }
                    logic.event_player_play_skipto(newTime);
                }
            }
        });

        // Search
        searchText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                logic.event_search_music(newValue);
            }
        });
        searchText.setPromptText("Search word...");
        searchResultsListView.setCellFactory(new Callback<ListView<MusicListItem>, ListCell<MusicListItem>>() {
            @Override
            public ListCell<MusicListItem> call(ListView<MusicListItem> listView) {
                // http://docs.oracle.com/javafx/2/ui_controls/list-view.htm
                return new ListItemCell(logic);
            }
        });
        // keys
        searchResultsListView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(final KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case ENTER:
                        ListItemManager.getInstance().play(logic, searchResultsListView, searchResultsListView.getSelectionModel().getSelectedItem());
                        break;
                    default:
                        break;
                }
            }
        });

        searchText.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.DOWN) {
                    searchResultsListView.getSelectionModel().clearAndSelect(0);
                    searchResultsListView.requestFocus();
                    searchResultsListView.getFocusModel().focus(0);
                }
            }

            ;
        });
        searchResultsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        enableListViewDragItems(searchResultsListView, TransferMode.COPY);

        // Resizer, if theme has a resizer pane
        if (resizer != null) {
            this.resizer.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    initX = mouseEvent.getScreenX();
                    initY = mouseEvent.getScreenY();

                    mouseEvent.consume();
                    musicListsAnchorContainer.setVisible(false);
                    musicListsAnchorContainer.setManaged(false);
                }
            });
            this.resizer.setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {

                    mouseEvent.consume();
                    musicListsAnchorContainer.setManaged(true);
                    musicListsAnchorContainer.setVisible(true);

                }
            });

            this.resizer.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    newX = mouseEvent.getScreenX();
                    newY = mouseEvent.getScreenY();
                    double deltax = newX - initX;
                    double deltay = newY - initY;

                    setStageWidth(stage, stage.getWidth() + deltax);
                    setStageHeight(stage, stage.getHeight() + deltay);

                    stageDimension.setWidth(stage.getWidth());
                    stageDimension.setHeight(stage.getHeight());

                    scene.getRoot().setClip(stageDimension);

                    mouseEvent.consume();

                    if (maximizeWindowIcon.getImage().equals(maximizeImage)) {
                        return;
                    }
                    maximizeWindowIcon.setImage(maximizeImage);
                }
            });
        }

        // Window icons
        this.closeWindowIcon.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                logic.close();
            }
        });

        // Equalizer
        this.equalizerIcon.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent m) {
                // Create equalizer dialog once
                if (equalizerStage == null) {
                    equalizerStage = DialogUtil.createDialog(stage, getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_equalizer.fxml"), false);
                    Pane root = (Pane) equalizerStage.getScene().getRoot();
                    final GridPane gridPane = (GridPane) root.lookup("#equalizerSliderGridPane");
                    final CheckBox equalizerOnOffCheckbox = (CheckBox) root.lookup("#equalizerOnOffCheckbox");
                    Slider patternSlider = (Slider) gridPane.getChildren().get(0);
                    gridPane.getChildren().clear();

                    // Create for each amp one slider
                    final Map<Integer, Slider> allSlider = new HashMap<Integer, Slider>();
                    int i = 0;
                    while (i < logic.equalizer_amps_amount()) {
                        Slider slider = new Slider();
                        slider.setMax(logic.equalizer_max_gain());
                        slider.setMin(logic.equalizer_min_gain());
                        slider.setValue(0);

                        // Clone attributes from the pattern slider
                        slider.setOrientation(patternSlider.getOrientation());
                        slider.setShowTickMarks(patternSlider.isShowTickMarks());
                        slider.setShowTickLabels(patternSlider.isShowTickLabels());
                        slider.setMajorTickUnit(patternSlider.getMajorTickUnit());
                        slider.setMinorTickCount(patternSlider.getMinorTickCount());

                        final int x = i;

                        slider.valueProperty().addListener(new ChangeListener<Number>() {
                            @Override
                            public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
                                logic.equalizer_set_amp(x, newValue.floatValue());
                            }
                        });

                        allSlider.put(i, slider);
                        gridPane.add(slider, i, 0);
                        i++;
                    }

                    // Create for each preset one choicebox entry
                    @SuppressWarnings("unchecked")
                    final ChoiceBox<String> choiceBox = (ChoiceBox<String>) root.lookup("#equalizerPresetsChoiceBox");
                    choiceBox.getItems().addAll(logic.equalizer_presets());
                    choiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

                        @Override
                        public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
                            if (Config.getInstance().EQUALIZER_ACTIVE) {
                                // Set preset amps on view
                                float[] amps = logic.equalizer_amps(choiceBox.getItems().get(newValue.intValue()));
                                int i = 0;
                                for (float f : amps) {
                                    Slider s = ((Slider) gridPane.getChildren().get(i++));
                                    s.setValue(f);
                                }

                                // Activate preset
                                logic.equalizer_set_amps(amps);
                            }
                        }
                    });
                    choiceBox.getSelectionModel().select(0);

                    // Equalizer on/off button
                    equalizerOnOffCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldValue, Boolean newValue) {
                            // ON
                            if (newValue) {
                                Config.getInstance().EQUALIZER_ACTIVE = true;
                                equalizerOnOffCheckbox.setText(Global.EQUALIZER_ON);
                                gridPane.setDisable(false);
                                choiceBox.setDisable(false);
                                float[] amps = new float[logic.equalizer_amps_amount()];
                                for (int i = 0; i < amps.length; i++) {
                                    amps[i] = allSlider.get(i).valueProperty().floatValue();
                                }
                                logic.equalizer_set_amps(amps);
                            }
                            // OFF
                            else {
                                Config.getInstance().EQUALIZER_ACTIVE = false;
                                equalizerOnOffCheckbox.setText(Global.EQUALIZER_OFF);
                                gridPane.setDisable(true);
                                choiceBox.setDisable(true);
                                logic.equalizer_disable();
                            }
                        }
                    });
                    if (Config.getInstance().EQUALIZER_ACTIVE) {
                        // Provoke a change event
                        equalizerOnOffCheckbox.selectedProperty().set(false);
                        equalizerOnOffCheckbox.selectedProperty().set(true);
                    } else {
                        equalizerOnOffCheckbox.selectedProperty().set(true);
                        equalizerOnOffCheckbox.selectedProperty().set(false);
                    }
                }

                equalizerStage.show();
            }

            ;
        });

        // Set version
        titleLabel.setText(MainFX.VERSION);
        artistBio.setText("Java: " + System.getProperty("java.version") + "\n" + "JavaFX: " + com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
    }

    public void setPlaybackMode(final PlayBackMode mode) {
        Platform.runLater(new Runnable() {
            public void run() {
                setPlayBackModeFX(mode);
                ListItemManager.getInstance().setPlayBackMode(mode);
            }
        });
    }

    public void setVolume(final int vol) {
        Platform.runLater(new Runnable() {
            public void run() {
                setVolumeFX(Config.getInstance().VOLUME, true);
            }
        });
    }

    public void setCurrentTotalDuration(final long value) {
        Platform.runLater(new Runnable() {
            public void run() {
                setDurationLength(value);
            }
        });
    }

    public void setCurrentDuration(final long value) {
        Platform.runLater(new Runnable() {
            public void run() {
                if (blockDurationProgress)
                    return;
                setDurationFX(value, true);
            }
        });
    }

    public void setPlayerState(final PlayerState pbm) {
        Platform.runLater(new Runnable() {
            public void run() {
                switch (pbm) {
                    case PLAYING:
                        playButtonImage.setImage(pauseImage);
                        playButtonImage.setTranslateX(0);
                        playButtonImage.setX(0);
                        break;
                    case PAUSED:
                        playButtonImage.setImage(playImage);
                        playButtonImage.setTranslateX(2);
                        playButtonImage.setX(20);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void initPlaylists(final LogicInterfaceFX logic, final ArrayList<Playlist> playlists, int activeIndex) {
        Platform.runLater(new Runnable() {
            public void run() {
                for (Playlist p : playlists) {
                    playlistTabs.getTabs().add(createPlayList(logic, p, false, false));
                }
                playlistTabs.getTabs().add(createNewTabButton(logic));
            }
        });
    }

    public void newPlaylist(final LogicInterfaceFX logic, final Playlist p) {
        Platform.runLater(new Runnable() {
            public void run() {
                int last = playlistTabs.getTabs().size() - 1;
                if (last < 0) {
                    last = 0;
                }
                playlistTabs.getTabs().add(last, createPlayList(logic, p, true, true));
                playlistTabs.getSelectionModel().select(last);
            }
        });
    }

    private Tab createNewTabButton(final LogicInterfaceFX logic) {
        final Tab newAddTabButton = new Tab("+");

        newAddTabButton.setOnSelectionChanged(new EventHandler<Event>() {
            @Override
            public void handle(Event t) {
                if (newAddTabButton.isSelected()) {
                    logic.event_playlist_new();
                }
            }
        });
        return newAddTabButton;
    }

    public synchronized void addMusicListItem(final String musicListViewId, final String rootFolder, final MusicListItem item) {
        final ListView<MusicListItem> listView = this.musicFolderListViews.get(musicListViewId);
        if (listView == null) {
            return;
        }

        // Determe index of list item
        int index = 0;
        for (MusicListItem i : listView.getItems()) {

            if (i.toString().compareTo(item.toString()) >= 0) {
                if (i instanceof MusicListItemFile && item instanceof MusicListItemFile) {
                    // If they have same parent folder, then item gets the same
                    // color
                    if (((MusicListItemFile) i).getFile().getParentFile().getAbsolutePath().equals(((MusicListItemFile) item).getFile().getParentFile().getAbsolutePath())) {
                        item.setColorClass(i.getColorClass());
                    }
                }
                break;
            }

            index++;
        }

        final int addIndex = index;
        Platform.runLater(new Runnable() {
            public void run() {
                listView.getItems().add(addIndex, item);
            }
        });
    }

    public void updateMusicFolderNotFound(final String tabLabel, final String id, final int pos) {
        Platform.runLater(new Runnable() {
            public void run() {

                Tab tab = musicFolderTabs.get(id);

                AnchorPane nodeFoundPane;
                try {
                    nodeFoundPane = FXMLLoader.load(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_music_folder_not_found.fxml"), languageBundle);
                    Label folderNotFoundLabelPath = (Label) nodeFoundPane.lookup("#folder_not_found_path");
                    folderNotFoundLabelPath.setText(id);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                tab.setContent(nodeFoundPane);
            }
        });
    }

    public void updateMusicFolderLoading(final int current, final int total, final MusicListItem item, final String id) {
        Platform.runLater(new Runnable() {
            public void run() {
                ProgressIndicator p = musicFolderListLoadingViews.get(id);

                if (p == null) {
                    return;
                }

                p.setProgress((double) current / (double) total);
                Text t = (Text) musicFolderListLoadingViews.get(id).lookup(".percentage");

                if (t == null) {
                    return;
                }

                if (current > 0) {
                    if (current == total) {
                        t.setText("Done");
                    }
                    t.setText(current + "/" + total);
                } else {
                    t.setText("Scanning files...");
                }
            }
        });
    }

    public void createLoadingMusicFolder(final String tabLabel, final String id, final int pos) {
        Platform.runLater(new Runnable() {
            public void run() {
                Tab tab = new Tab();
                tab.setText(tabLabel);
                tab.setId(id);
                AnchorPane loadingPane = null;
                try {
                    loadingPane = (AnchorPane) FXMLLoader.load(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_music_folder_loading.fxml"), languageBundle);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                VBox vbox = (VBox) loadingPane.getChildren().get(0);
                ProgressIndicator indicator = (ProgressIndicator) vbox.getChildren().get(0);
                tab.setContent(loadingPane);

                musicFolderTabs.put(id, tab);
                musicFolderListLoadingViews.put(id, indicator);
                musicTabs.getTabs().add(pos, tab);
                musicTabs.getSelectionModel().select(0);
            }
        });
    }

    public void createMusicFolder(final LogicInterfaceFX logic, final String tabLabel, final String id, final int pos, final ArrayList<MusicListItem> items) {
        Platform.runLater(new Runnable() {
            public void run() {
                Tab tab = new Tab();
                tab.setText(tabLabel);
                tab.setId(id);
                AnchorPane musicPane = null;
                try {
                    musicPane = (AnchorPane) FXMLLoader.load(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_list.fxml"));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                ListView<MusicListItem> listView = prepareMusicListPane(logic, id, items, musicPane);
                tab.setContent(listView);

                musicFolderListViews.put(id, listView);
                musicTabs.getTabs().add(pos, tab);
                musicTabs.getSelectionModel().select(0);
            }
        });
    }

    public void setMusicFolder(final LogicInterfaceFX logic, final String t, final String id, final int pos, final ArrayList<MusicListItem> items) {
        Platform.runLater(new Runnable() {
            public void run() {
                AnchorPane listPane = null;
                try {
                    listPane = (AnchorPane) FXMLLoader.load(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_list.fxml"));
                    musicFolderTabs.get(id).setContent(listPane);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                ListView<MusicListItem> listView = prepareMusicListPane(logic, id, items, listPane);
                musicFolderListViews.put(id, listView);
            }
        });
    }

    private ListView<MusicListItem> prepareMusicListPane(final LogicInterfaceFX logic, final String id, final ArrayList<MusicListItem> items, AnchorPane listPane) {
        @SuppressWarnings("unchecked")
        final ListView<MusicListItem> listView = (ListView<MusicListItem>) listPane.getChildren().get(0);

        ObservableList<MusicListItem> list = FXCollections.observableList(items);

        listView.setItems(list);

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setId(id);

        // ListCell
        listView.setCellFactory(new Callback<ListView<MusicListItem>, ListCell<MusicListItem>>() {
            @Override
            public ListCell<MusicListItem> call(ListView<MusicListItem> listView) {
                // http://docs.oracle.com/javafx/2/ui_controls/list-view.htm
                return new MusicListItemCell(logic);
            }
        });

        // Enable draggable itmes
        enableListViewDragItems(listView, TransferMode.COPY);

        // keys
        listView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(final KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case ENTER:
                        ListItemManager.getInstance().play(logic, listView, listView.getSelectionModel().getSelectedItem());
                        break;
                    default:
                        break;
                }
            }
        });

        return listView;
    }

    private Tab createPlayList(final LogicInterfaceFX logic, final Playlist playlist, boolean selectTitle, boolean activateTab) {
        Tab tab = TabUtil.createEditableTab(playlist.getId(), playlist.getName(), selectTitle, logic);

        // get layout
        AnchorPane listPane = null;
        try {
            listPane = (AnchorPane) FXMLLoader.load(getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_list.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // create list view
        @SuppressWarnings("unchecked")
        final ListView<MusicListItem> listView = (ListView<MusicListItem>) listPane.getChildren().get(0);
        listView.setItems(FXCollections.observableList(playlist.getItems()));
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        playListViews.add(listView);

        final ListItemDragHandler dragHandler = new ListItemDragHandler();

        // select tab
        if (activateTab) {
            ListItemManager.getInstance().setCurrentListView(listView);
        }

        enableListViewDragItems(listView, TransferMode.MOVE);

        // item drag and drop
        listView.setCellFactory(new Callback<ListView<MusicListItem>, ListCell<MusicListItem>>() {
            @Override
            public ListCell<MusicListItem> call(ListView<MusicListItem> item) {
                ListItemCell cell = new ListItemCell(logic);
                cell.setOnDragOver(dragHandler);
                cell.setOnDragEntered(dragHandler);
                cell.setOnDragExited(dragHandler);
                cell.setOnDragDropped(dragHandler);
                return cell;
            }
        });

        listView.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.ANY);
                event.consume();
            }
        });

        listView.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {

                // calculate drop index
                int dropIndex = dragHandler.getCurrentIndex();
                if (dropIndex < listView.getItems().size()) {
                    if (dragHandler.isBefore()) {
                        // this is ok
                    } else {
                        dropIndex = dropIndex + 1;
                    }
                } else {
                    dropIndex = listView.getItems().size();
                }

                String url = event.getDragboard().getString();
                ArrayList<MusicListItem> items;

                // URL detected
                if (url != null) {
                    items = new ArrayList<MusicListItem>(1);
                    MusicListItem item = logic.event_playlist_url_dropped(url, playlist);
                    items.add(item);
                }
                // MusicListItem
                else {
                    items = (ArrayList<MusicListItem>) event.getDragboard().getContent(DATA_FORMAT_LIST);
                }

                switch (event.getTransferMode()) {
                /* ############# COPY ############# */
                    case COPY:
                        if (items != null) {
                            listView.getSelectionModel().clearSelection();
                            listView.getItems().addAll(dropIndex, items);
                            listView.getSelectionModel().selectRange(dropIndex, dropIndex + items.size());
                            event.setDropCompleted(true);
                            event.consume();
                            logic.event_playlist_changed(playlist);
                        }
                        break;

				/* ############# MOVE ############# */
                    case MOVE:

                        // URL
                        if (url != null) {
                            listView.getItems().addAll(dropIndex, items);
                            return;
                        }

                        // save selection temporarily
                        int[] indices = new int[listView.getSelectionModel().getSelectedIndices().size()];
                        int j = 0;
                        for (Integer i : listView.getSelectionModel().getSelectedIndices()) {
                            indices[j] = i;
                            ++j;
                        }

                        // add items
                        listView.getItems().addAll(dropIndex, listView.getSelectionModel().getSelectedItems());

                        // clear selection
                        listView.getSelectionModel().clearSelection();

                        // change indices to remove items
                        int amountSmallerDropIndex = 0;
                        for (int i = 0; i < indices.length; i++) {
                            if (indices[i] >= dropIndex) {
                                indices[i] = indices[i] + indices.length;
                            } else {
                                amountSmallerDropIndex++;
                            }
                        }

                        // remove items
                        for (int i = indices.length - 1; i >= 0; i--) {
                            listView.getItems().remove(indices[i]);
                        }

                        // select moved items
                        listView.getSelectionModel().selectRange(dropIndex - amountSmallerDropIndex, dropIndex - amountSmallerDropIndex + indices.length);

                        break;
                    default:
                        break;
                }
            }
        });

        // keys
        final EventHandler<KeyEvent> playlistKeyEventHandler = new EventHandler<KeyEvent>() {
            public void handle(final KeyEvent keyEvent) {

                switch (keyEvent.getCode()) {
                    case DELETE:
                        Platform.runLater(new Runnable() {
                            public void run() {
                                // Delete all
                                if (listView.getSelectionModel().getSelectedItems().size() == listView.getItems().size()) {
                                    listView.getItems().clear();
                                    keyEvent.consume();
                                    logic.event_playlist_changed(playlist);
                                    return;
                                }

                                // Delete by index
                                int firstIndex = listView.getSelectionModel().getSelectedIndex();
                                ObservableList<MusicListItem> delete = FXCollections.observableArrayList(listView.getSelectionModel().getSelectedItems());
                                listView.getItems().removeAll(delete);
                                listView.getSelectionModel().clearSelection();

                                // last item was removed, that isn't available
                                // anymore => Reset index
                                if (firstIndex >= listView.getItems().size()) {
                                    firstIndex = listView.getItems().size() - 1;
                                }
                                listView.getSelectionModel().select(firstIndex);

                                keyEvent.consume();
                                logic.event_playlist_changed(playlist);

                            }
                        });

                        break;

                    case ENTER:
                        ListItemManager.getInstance().play(logic, listView, listView.getSelectionModel().getSelectedItem());
                        break;
                    default:
                        break;
                }

            }
        };
        listView.addEventHandler(KeyEvent.KEY_PRESSED, playlistKeyEventHandler);
        tab.setContent(listPane);

        return tab;
    }

    private void enableListViewDragItems(final ListView<MusicListItem> listView, final TransferMode mode) {
        // Drag
        listView.setOnDragDetected(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                /* allow any transfer mode */
                Dragboard db = listView.startDragAndDrop(mode);

				/* put a string on dragboard */
                ClipboardContent content = new ClipboardContent();
                content.clear();

                content.put(DATA_FORMAT_LIST, new ArrayList<MusicListItem>(listView.getSelectionModel().getSelectedItems()));

                db.setContent(content);
                event.consume();
            }
        });
    }

    public void next(LogicInterfaceFX logic) {
        ListItemManager.getInstance().next(logic);
    }

    public void prev(LogicInterfaceFX logic) {
        ListItemManager.getInstance().prev(logic);
    }

    public static class MusicListItemCell extends ListItemCell {

        public MusicListItemCell(LogicInterfaceFX logic) {
            super(logic);
        }

        @Override
        public void updateItem(MusicListItem item, boolean empty) {
            super.updateItem(item, empty);

            if (Config.getInstance().COLORIZE_ITEMS && !empty) {
                this.getStyleClass().clear();
                this.getStyleClass().add("c" + item.getColorClass());
                this.getStyleClass().addAll(styleClasses);
                this.getStyleClass().add(defaultCellClass);
            }
        }
    }

    @Override
    public void showSearchResults(final ArrayList<MusicListItem> searchResult, final int counter) {
        if (Platform.isFxApplicationThread()) {
            createSearchResults(searchResult, counter);
            return;
        }

        // Just show it if it's not out of date
        Platform.runLater(new Runnable() {
            public void run() {
                createSearchResults(searchResult, counter);
            }
        });
    }

    private void createSearchResults(final ArrayList<MusicListItem> searchResult, final int counter) {
        if (counter > searchResultCounter) {
            searchResultCounter = counter;
            searchResultsListView.getItems().clear();
            if (searchResult != null && searchResult.size() > 0) {
                searchResultsListView.getItems().addAll(searchResult);
            }
        }
    }

    public void showInformation(final MusicListItem item) {
        Platform.runLater(new Runnable() {

            public void run() {
                resetView();
                setImage(item.getMainImage());
                setHeading(item.getHeading(), "");
                setSubheading(item.getSubheading());
                setText(item.getText());
                setLinks(item.getSubLinks());
            }

            private void setText(String text) {
                if (text == null) {
                    return;
                }
                artistBio.setText(Jsoup.parse(text).text());
                artistBio.setVisible(true);
                artistBio.setManaged(true);
            }

            private void setSubheading(String text) {
                if (text == null) {
                    return;
                }

                titleLabel.setText(text);
                titleLabel.setManaged(true);
                titleLabel.setVisible(true);
            }

            private void setHeading(String text, String alternativeText) {
                if (text == null) {
                    text = alternativeText;
                }

                if (text == null) {
                    return;
                }

                artistLabel.setText(text);
                artistLabel.setManaged(true);
                artistLabel.setVisible(true);
            }

            private void setLinks(List<Link> links) {
                if (links == null || links.size() == 0) {
                    return;
                }

                topTracks.setVisible(true);

                int trackCounter = 0;

                for (Link link : links) {
                    Hyperlink h = new Hyperlink(link.getText());


                    final String url = link.getHref();
                    h.setTooltip(new Tooltip(url));

                    h.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent e) {
                            try {
                                BrowserUtil.openWebpage(new URL(url));
                            } catch (MalformedURLException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    topTracks.getChildren().add(h);
                    ++trackCounter;
                    if (trackCounter == 32) {
                        break;
                    }
                }
            }

            private void setImage(String artistImagePath) {
                if (artistImagePath == null) {
                    return;
                }

                Logger.get().info("Artist image path: " + artistImagePath);
                File f = new File(artistImagePath);

                // image exists
                if (f.exists()) {
                    try {
                        Image img = new Image(new FileInputStream(f));
                        artistImage.setImage(img);
                        imageContainer.setVisible(true);
                        imageContainer.setManaged(true);
                        artistImage.setVisible(true);
                        artistImage.setManaged(true);
                    } catch (FileNotFoundException e2) {
                        e2.printStackTrace();
                    }
                }
            }

            private void resetView() {
                artistLabel.setText("");
                artistLabel.setManaged(false);
                artistLabel.setVisible(false);
                artistBio.setText("");
                artistBio.setManaged(false);
                artistBio.setVisible(false);
                imageContainer.setVisible(false);
                imageContainer.setManaged(false);
                artistImage.setVisible(false);
                artistImage.setManaged(false);
                topTracks.getChildren().clear();
                topTracks.setVisible(false);
                titleLabel.setText("");
                titleLabel.setVisible(false);
                titleLabel.setManaged(false);
            }
        });
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public Scene getScene() {
        return this.scene;
    }

    protected TabPane getPlaylistTabs() {
        return this.playlistTabs;
    }

    public void showStreamSelectionDialog(final LogicInterfaceFX logic, final MusicListItemYoutube item) {
        Platform.runLater(new Runnable() {
            public void run() {

                final Stage dialog = DialogUtil.createDialog(stage, getClass().getClassLoader().getResource(ThemeUtils.getThemePath() + "/layout_select_stream_type_dialog.fxml"), false, languageBundle);
                VBox container = (VBox) dialog.getScene().lookup("#streamTypes");
                Button ok = (Button) dialog.getScene().lookup("#okButton");
                Button abort = (Button) dialog.getScene().lookup("#abortButton");
                Label message = (Label) dialog.getScene().lookup("#confirmMessage");

                ArrayList<YoutubeVideoStreamURL> audio = new ArrayList<YoutubeVideoStreamURL>();
                ArrayList<YoutubeVideoStreamURL> video = new ArrayList<YoutubeVideoStreamURL>();
                ArrayList<YoutubeVideoStreamURL> audio_video = new ArrayList<YoutubeVideoStreamURL>();

                for (YoutubeVideoStreamURL stream : item.getStreams()) {
                    if (stream.getStreamType() == StreamType.AUDIO) {
                        audio.add(stream);
                        continue;
                    }

                    if (stream.getStreamType() == StreamType.VIDEO) {
                        video.add(stream);
                        continue;
                    }

                    if (stream.getStreamType() == StreamType.VIDEO_AUDIO) {
                        audio_video.add(stream);
                        continue;
                    }
                }

                final ToggleGroup group = new ToggleGroup();

                Insets headingPadding  = new Insets(18, 0, 6, 0);

                if (audio.size() > 0) {
                    Label audioHeading = new Label("Audio");
                    audioHeading.getStyleClass().add("heading1");
                    audioHeading.setPadding(headingPadding);
                    container.getChildren().add(audioHeading);
                    for (YoutubeVideoStreamURL stream : audio) {
                        RadioButton r = new RadioButton(stream.toString());
                        r.setToggleGroup(group);
                        r.setId(stream.getUrl());
                        container.getChildren().add(r);
                    }
                }

                if (video.size() > 0) {
                    Label videoHeading = new Label("Video");
                    videoHeading.getStyleClass().add("heading1");
                    videoHeading.setPadding(headingPadding);
                    container.getChildren().add(videoHeading);
                    for (YoutubeVideoStreamURL stream : video) {
                        RadioButton r = new RadioButton(stream.toString());
                        r.setId(stream.getUrl());
                        r.setToggleGroup(group);
                        container.getChildren().add(r);
                    }
                }

                if (audio_video.size() > 0) {
                    Label videoAudioHeading = new Label("Audio and Video");
                    videoAudioHeading.getStyleClass().add("heading1");
                    videoAudioHeading.setPadding(headingPadding);
                    container.getChildren().add(videoAudioHeading);
                    for (YoutubeVideoStreamURL stream : audio_video) {
                        RadioButton r = new RadioButton(stream.toString());
                        r.setToggleGroup(group);
                        r.setId(stream.getUrl());
                        container.getChildren().add(r);
                    }
                }


                ok.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        dialog.hide();
                        RadioButton r = (RadioButton) group.getSelectedToggle();

                        for(YoutubeVideoStreamURL youtubeStreamURL: item.getStreams()){
                            if (youtubeStreamURL.getUrl().equals(r.getId())){
                                item.setSelectedVideoStream(youtubeStreamURL);
                                logic.event_stream_selected(item);
                                return;
                            }
                        }
                    }
                });

                abort.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        dialog.hide();
                    }
                });


                dialog.show();
                AlignmentUtil.center(stage, dialog);
            }
        });
    }

    public static class ListItemManager {
        private Random random;
        private int currentItemIndexGuess;
        private MusicListItem currentItem;
        private PlayBackMode playBackMode;
        private ListView<MusicListItem> currentListView;
        private static ListItemManager instance = new ListItemManager();

        public ListItemManager() {
            this.random = new Random();
        }

        public void next(final LogicInterfaceFX logic) {
            final ListView<MusicListItem> clv = this.currentListView;
            final MusicListItem ci = this.currentItem;

            // Fix: That the slider isn't toggling if length changed
            if (Platform.isFxApplicationThread()) {
                ViewManagerFX.getInstance().getController().durationSlider.setValue(0);
                ViewManagerFX.getInstance().getController().durationProgress.setProgress(0);
            } else {
                Platform.runLater(new Runnable() {
                    public void run() {
                        ViewManagerFX.getInstance().getController().durationSlider.setValue(0);
                        ViewManagerFX.getInstance().getController().durationProgress.setProgress(0);
                    }
                });
            }

            if (clv == null) {
                return; // fuck my life, do nothing
            }

            // Current index
            int oldIndex = -1;
            if (ci == clv.getItems().get(currentItemIndexGuess)) {
                oldIndex = currentItemIndexGuess;
            } else {
                oldIndex = clv.getItems().indexOf(ci);
            }

            // Calc next index
            final int nextIndex = getNextIndex(oldIndex);
            final MusicListItem nextItem = currentListView.getItems().get(nextIndex);
            final MusicListItem oldItem = currentItem;

            if (Platform.isFxApplicationThread()) {
                switchTo(nextIndex, nextItem);
            } else {
                Platform.runLater(new Runnable() {
                    public void run() {
                        switchTo(nextIndex, nextItem);
                    }
                });
            }

            logic.event_player_next(oldItem, nextItem);
        }

        public void prev(LogicInterfaceFX logic) {
            if (this.currentListView == null) {
                return; // fuck my life, do nothing
            }

            // Current index
            int oldIndex = -1;
            if (this.currentItem == this.currentListView.getItems().get(currentItemIndexGuess)) {
                oldIndex = currentItemIndexGuess;
            } else {
                oldIndex = this.currentListView.getItems().indexOf(this.currentItem);
            }

            // Calc prev index
            int tmpNextIndex = (oldIndex - 1) % this.currentListView.getItems().size();
            if (tmpNextIndex < 0) {
                tmpNextIndex = this.currentListView.getItems().size() - 1;
            }
            final int prevIndex = tmpNextIndex;
            final MusicListItem nextItem = currentListView.getItems().get(prevIndex);
            final MusicListItem oldItem = currentItem;

            if (Platform.isFxApplicationThread()) {
                switchTo(prevIndex, nextItem);
            } else {
                Platform.runLater(new Runnable() {
                    public void run() {
                        switchTo(prevIndex, nextItem);
                    }
                });
            }

            logic.event_player_previous(oldItem, nextItem);
        }

        private void switchTo(int newIndex, MusicListItem newItem) {
            currentItem.setStatus(PlaybackStatus.SET_NONE);
            currentListView.scrollTo(newIndex - 5 < 0 ? 0 : newIndex - 5);
            newItem.setStatus(PlaybackStatus.SET_PLAYING);

            setCurrentItem(newItem);
            setCurrentListView(currentListView);
        }

        public void play(LogicInterfaceFX logic, final ListView<MusicListItem> playMeListView, final MusicListItem playMe) {
            ViewManagerFX.getInstance().getController().durationSlider.setValue(0);
            ViewManagerFX.getInstance().getController().durationProgress.setProgress(0);

            final ListView<MusicListItem> listViewOld = ListItemManager.getInstance().getCurrentListView();

            // Is there an item that was played befored?
            MusicListItem oldItem = null;
            if (listViewOld != null) {
                oldItem = ListItemManager.getInstance().getCurrentItem();
                if (oldItem != null) {
                    oldItem.setStatus(PlaybackStatus.SET_NONE);
                }
            }

            final int index = playMeListView.getItems().indexOf(playMe);
            if (playMe == null)
                return;
            playMe.setStatus(PlaybackStatus.SET_PLAYING);
            Platform.runLater(new Runnable() {
                public void run() {
                    setCurrentListView(playMeListView);
                    setCurrentItem(playMe);

                    playMeListView.getSelectionModel().clearSelection();
                    playMeListView.getSelectionModel().select(index);
                    currentListView.getFocusModel().focus(index);
                }
            });
            logic.event_player_next(oldItem, playMe);
        }

        public int getNextIndex(int oldIndex) {
            int nextIndex = -1;

            switch (this.playBackMode) {
                case NORMAL:
                    nextIndex = (oldIndex + 1) % this.currentListView.getItems().size();
                    break;
                case SHUFFLE:
                    nextIndex = random.nextInt(this.currentListView.getItems().size());

                    // not the same song again pls...
                    if (nextIndex == oldIndex) {
                        // ok then play the next song
                        nextIndex = (nextIndex + 1) % this.currentListView.getItems().size();
                    }
                    break;
                case REPEAT:
                    nextIndex = oldIndex;
                    break;
            }
            return nextIndex;
        }

        public static ListItemManager getInstance() {
            return instance;
        }

        public MusicListItem getCurrentItem() {
            return this.currentItem;
        }

        public void setCurrentItem(MusicListItem item) {
            this.currentItem = item;
        }

        public ListView<MusicListItem> getCurrentListView() {
            return currentListView;
        }

        public void setCurrentListView(ListView<MusicListItem> currentListView) {
            this.currentListView = currentListView;
        }

        public PlayBackMode getPlayBackMode() {
            return playBackMode;
        }

        public void setPlayBackMode(PlayBackMode playBackMode) {
            this.playBackMode = playBackMode;
        }
    }


    public String getSearchText() {
        return this.searchText.getText();
    }
}