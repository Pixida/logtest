/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.testrun;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.json.JSONException;
import org.json.JSONObject;

import de.pixida.logtest.automatondefinitions.JsonAutomatonDefinition;
import de.pixida.logtest.designer.Editor;
import de.pixida.logtest.designer.IMainWindow;
import de.pixida.logtest.designer.commons.ExceptionDialog;
import de.pixida.logtest.designer.commons.Icons;
import de.pixida.logtest.designer.commons.SelectFileButton;
import de.pixida.logtest.designer.logreader.LogReaderEditor;
import de.pixida.logtest.engine.AutomatonParameters;
import de.pixida.logtest.logreaders.GenericLogReader;
import de.pixida.logtest.processing.EvaluationResult;
import de.pixida.logtest.processing.EvaluationResult.Result;
import de.pixida.logtest.processing.Job;
import de.pixida.logtest.processing.JobExecutor;
import de.pixida.logtest.processing.LogSink;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class TestRunEditor extends Editor
{
    private static final Background RESULT_BAR_BACKGROUND_IDLE = new Background(new BackgroundFill(
        Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background RESULT_BAR_BACKGROUND_PROGRESS = new Background(new BackgroundFill(
        Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background RESULT_BAR_BACKGROUND_SUCCESS = new Background(new BackgroundFill(
        Color.GREEN.brighter().brighter().desaturate().desaturate().desaturate(), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background RESULT_BAR_BACKGROUND_AUTOMATON_DEFINITION_ERROR = new Background(new BackgroundFill(
        Color.YELLOW.brighter().brighter().desaturate(), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background RESULT_BAR_BACKGROUND_INTERNAL_ERROR = new Background(new BackgroundFill(
        Color.GREY.brighter(), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background RESULT_BAR_BACKGROUND_TEST_FAILURE = new Background(new BackgroundFill(
        Color.RED.brighter().brighter().desaturate(), CornerRadii.EMPTY, Insets.EMPTY));

    private final SimpleBooleanProperty showDebugOutputProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty logFilePathProperty = new SimpleStringProperty();
    private final SimpleBooleanProperty loadLogFromEnteredTextProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty loadLogFromFileProperty = new SimpleBooleanProperty();
    private final SimpleStringProperty enteredLogTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty automatonFilePathProperty = new SimpleStringProperty();
    private final SimpleStringProperty logReaderConfigurationFilePathProperty = new SimpleStringProperty();
    private final SimpleStringProperty parametersProperty = new SimpleStringProperty();
    private final SimpleObjectProperty<Background> resultBarBackgroundProperty = new SimpleObjectProperty<>();
    private final SimpleStringProperty resultBarTextProperty = new SimpleStringProperty();

    private final TextArea resultLogOutputText = new TextArea();

    private class AutomatonLoggerListener extends AppenderSkeleton
    {
        AutomatonLoggerListener()
        {
            // Empty constructor needed by checkstyle
        }

        @Override
        public void close()
        {
        }

        @Override
        public boolean requiresLayout()
        {
            return false;
        }

        @Override
        protected void append(final LoggingEvent event)
        {
            Platform.runLater(() -> {
                TestRunEditor.this.resultLogOutputText.appendText(String.format("[%s] ", event.getLevel()));
                TestRunEditor.this.resultLogOutputText.appendText(event.getMessage().toString());
                TestRunEditor.this.resultLogOutputText.appendText("\n");
            });
        }
    }

    private final AutomatonTestRunService testRunService = new AutomatonTestRunService();

    private class AutomatonTestRunService extends Service<EvaluationResult>
    {
        private Job job;
        private final Logger engineLogger = Logger.getLogger("de.pixida.logtest");

        AutomatonTestRunService()
        {
            // Empty constructor needed by checkstyle
        }

        protected void setJob(final Job value)
        {
            this.job = value;
        }

        @Override
        protected Task<EvaluationResult> createTask()
        {
            return new Task<EvaluationResult>()
            {
                @Override
                protected EvaluationResult call()
                {
                    AutomatonTestRunService.this.engineLogger
                        .setLevel(TestRunEditor.this.showDebugOutputProperty.get() ? Level.DEBUG : Level.INFO);
                    final AutomatonLoggerListener logHook = new AutomatonLoggerListener();
                    TestRunEditor.this.resultLogOutputText.setText("");
                    AutomatonTestRunService.this.engineLogger.addAppender(logHook);

                    try
                    {
                        final JobExecutor executor = new JobExecutor(Arrays.asList(AutomatonTestRunService.this.job));
                        final EvaluationResult result = executor.getResults().get(0).get(0);

                        if (result.getResult() == Result.INTERNAL_ERROR)
                        {
                            throw new RuntimeException(result.getMessage());
                        }

                        return result;
                    }
                    finally
                    {
                        AutomatonTestRunService.this.engineLogger.removeAppender(logHook);
                    }
                }

            };
        }
    };

    public TestRunEditor(final IMainWindow mainWindow)
    {
        super(Editor.Type.TEST_RUN, mainWindow);
    }

    @Override
    protected void init()
    {
        this.testRunService.setOnScheduled(event -> {
            this.resultBarTextProperty.set("Running ...");
            this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_PROGRESS);
        });
        this.testRunService.setOnFailed(event -> {
            final Throwable ex = event.getSource().getException();
            final String errorMsg = ex != null ? ex.getMessage() : "Unknown internal error.";
            this.resultBarTextProperty.set(errorMsg);
            this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_INTERNAL_ERROR);
            ExceptionDialog.showFatalException("An error occured while running the automaton", errorMsg, ex);
            this.testRunService.reset();
        });
        this.testRunService.setOnSucceeded(event -> {
            final Object resultObj = event.getSource().getValue();
            if (resultObj instanceof EvaluationResult)
            {
                final EvaluationResult result = (EvaluationResult) resultObj;

                String msg;
                if (result.getResult() == Result.AUTOMATON_DEFECT)
                {
                    msg = "Invalid automaton definition.";
                    this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_AUTOMATON_DEFINITION_ERROR);
                }
                else if (result.getResult() == Result.FAILURE)
                {
                    msg = "Test FAILURE.";
                    this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_TEST_FAILURE);
                }
                else if (result.getResult() == Result.SUCCESS)
                {
                    msg = "Test SUCCEEDED.";
                    this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_SUCCESS);
                }
                else
                {
                    msg = "Unknown result.";
                    this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_INTERNAL_ERROR);
                }

                if (StringUtils.isNotBlank(result.getMessage()))
                {
                    msg += " " + result.getMessage();
                }

                this.resultBarTextProperty.set(msg);
            }
            this.testRunService.reset();
        });
        this.createDialogItems();
    }

    private void createDialogItems()
    {
        final TitledPane configPane = this.createPanelForConfiguration();
        final TitledPane runPane = this.createPanelForLaunchingTests();
        final VBox panes = new VBox(configPane, runPane);
        final double insetsOfScrollPane = 10d;
        final double spacingOfPanes = insetsOfScrollPane;
        panes.setSpacing(spacingOfPanes);
        final ScrollPane sp = new ScrollPane(panes);
        sp.setPadding(new Insets(insetsOfScrollPane));
        sp.setFitToWidth(true);
        this.setCenter(sp);
    }

    public TitledPane createPanelForConfiguration()
    {
        final GridPane gp = new GridPane();
        gp.setAlignment(Pos.BASELINE_LEFT);
        final double hGapOfGridPane = 10d;
        gp.setHgap(hGapOfGridPane);
        final double vGapOfGridPane = 15d;
        gp.setVgap(vGapOfGridPane);
        final double paddingOfGridPane = 5d;
        gp.setPadding(new Insets(paddingOfGridPane));
        final ColumnConstraints column1 = new ColumnConstraints();
        final ColumnConstraints column2 = new ColumnConstraints();
        column1.setHgrow(Priority.NEVER);
        column2.setHgrow(Priority.SOMETIMES);
        gp.getColumnConstraints().addAll(column1, column2);
        this.insertConfigItemsIntoGrid(gp, this.createConfigurationForm());
        final TitledPane configPane = new TitledPane("Edit Configuration", gp);
        configPane.setGraphic(Icons.getIconGraphics("pencil"));
        configPane.setCollapsible(false);
        return configPane;
    }

    public TitledPane createPanelForLaunchingTests()
    {
        final Button startBtn = new Button("Run Test");
        startBtn.disableProperty().bind(this.testRunService.runningProperty());
        final double startButtonPadding = 8d;
        startBtn.setPadding(new Insets(startButtonPadding));
        startBtn.setGraphic(Icons.getIconGraphics("control_play_blue"));
        HBox.setHgrow(startBtn, Priority.ALWAYS);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(event -> {
            final Job job = this.createJobFromConfig();
            this.testRunService.setJob(job);
            this.testRunService.start();
        });
        final HBox startLine = new HBox();
        startLine.getChildren().add(startBtn);
        final VBox runLines = new VBox();
        final double linesSpacing = 10d;
        runLines.setSpacing(linesSpacing);
        final TextFlow resultBar = new TextFlow();
        resultBar.backgroundProperty().bind(this.resultBarBackgroundProperty);
        this.resultBarBackgroundProperty.set(RESULT_BAR_BACKGROUND_IDLE);
        resultBar.setStyle("-fx-border-color: black; -fx-border-width:1");
        final Text resultBarText = new Text();
        resultBarText.textProperty().bind(this.resultBarTextProperty);
        this.resultBarTextProperty.set("Idle");
        resultBar.getChildren().add(resultBarText);
        resultBar.setTextAlignment(TextAlignment.CENTER);
        final double resultBarPadding = 2d;
        resultBar.setPadding(new Insets(resultBarPadding));
        final int logOutputLinesSize = 25;
        this.resultLogOutputText.setPrefRowCount(logOutputLinesSize);
        this.resultLogOutputText.setEditable(false);
        this.resultLogOutputText.setStyle("-fx-font-family: monospace");
        HBox.setHgrow(this.resultLogOutputText, Priority.ALWAYS);
        runLines.getChildren().addAll(startLine, new Text("Recent results:"), resultBar, this.resultLogOutputText);
        final TitledPane runPane = new TitledPane("Run", runLines);
        runPane.setGraphic(Icons.getIconGraphics("lightning_go"));
        runPane.setCollapsible(false);
        return runPane;
    }

    private List<Triple<String, Node, String>> createConfigurationForm()
    {
        final List<Triple<String, Node, String>> formItems = new ArrayList<>();

        // Automaton file
        final TextField automatonFilePath = new TextField();
        this.automatonFilePathProperty.bind(automatonFilePath.textProperty());
        HBox.setHgrow(automatonFilePath, Priority.ALWAYS);
        final Button selectAutomatonFilePathButton = SelectFileButton.createButtonWithFileSelection(automatonFilePath,
            Editor.Type.AUTOMATON.getIconName(), "Select " + Editor.Type.AUTOMATON.getName(), Editor.Type.AUTOMATON.getFileMask(),
            Editor.Type.AUTOMATON.getFileDescription());
        final HBox automatonFilePathConfig = new HBox(automatonFilePath, selectAutomatonFilePathButton);
        formItems.add(Triple.of("Automaton", automatonFilePathConfig, null));

        // Automaton parameters
        final TextArea parametersInputText = new TextArea();
        parametersInputText.setStyle("-fx-font-family: monospace");
        HBox.setHgrow(parametersInputText, Priority.ALWAYS);
        final int parametersInputLinesSize = 4;
        parametersInputText.setPrefRowCount(parametersInputLinesSize);
        this.parametersProperty.bind(parametersInputText.textProperty());
        formItems.add(Triple.of("Parameters", parametersInputText,
            "Set parameters for the execution. Each line can contain a parameter as follows: ${PARAMETER}=value, e.g. ${TIMEOUT}=10."));

        // Log file
        this.createLogFileSourceInputItems(formItems);

        // Log reader configuration file
        final TextField logReaderConfigurationFilePath = new TextField();
        this.logReaderConfigurationFilePathProperty.bind(logReaderConfigurationFilePath.textProperty());
        HBox.setHgrow(logReaderConfigurationFilePath, Priority.ALWAYS);
        final Button selectLogReaderConfigurationFilePathButton = SelectFileButton.createButtonWithFileSelection(
            logReaderConfigurationFilePath,
            Editor.Type.LOG_READER_CONFIG.getIconName(), "Select " + Editor.Type.LOG_READER_CONFIG.getName(),
            Editor.Type.LOG_READER_CONFIG.getFileMask(), Editor.Type.LOG_READER_CONFIG.getFileDescription());
        final HBox logReaderConfigurationFilePathConfig = new HBox(logReaderConfigurationFilePath,
            selectLogReaderConfigurationFilePathButton);
        formItems.add(Triple.of("Log Reader Configuration", logReaderConfigurationFilePathConfig, null));

        // Debug output
        final CheckBox cb = new CheckBox();
        this.showDebugOutputProperty.bind(cb.selectedProperty());
        formItems.add(Triple.of("Show Debug Output", cb, "Show verbose debug output. Might generate lots of text and slows down the"
            + " evaluation, but is very helpful for debugging automatons while developing them."));

        return formItems;
    }

    public void createLogFileSourceInputItems(final List<Triple<String, Node, String>> formItems)
    {
        final TextField logFilePath = new TextField();
        this.logFilePathProperty.bind(logFilePath.textProperty());
        HBox.setHgrow(logFilePath, Priority.ALWAYS);
        final Button selectLogFileButton = SelectFileButton.createButtonWithFileSelection(logFilePath, LogReaderEditor.LOG_FILE_ICON_NAME,
            "Select log file", null, null);
        final HBox fileInputConfig = new HBox(logFilePath, selectLogFileButton);
        final VBox lines = new VBox();
        final double spacingOfLines = 5d;
        lines.setSpacing(spacingOfLines);
        final HBox inputTypeLine = new HBox();
        final double hSpacingOfInputTypeChoices = 30d;
        inputTypeLine.setSpacing(hSpacingOfInputTypeChoices);
        final ToggleGroup group = new ToggleGroup();
        final RadioButton inputTypeText = new RadioButton("Paste/Enter log");
        inputTypeText.setToggleGroup(group);
        this.loadLogFromEnteredTextProperty.bind(inputTypeText.selectedProperty());
        final RadioButton inputTypeFile = new RadioButton("Read log file");
        inputTypeFile.setToggleGroup(group);
        this.loadLogFromFileProperty.bind(inputTypeFile.selectedProperty());
        inputTypeFile.setSelected(true);
        inputTypeLine.getChildren().add(inputTypeText);
        inputTypeLine.getChildren().add(inputTypeFile);
        fileInputConfig.visibleProperty().bind(inputTypeFile.selectedProperty());
        fileInputConfig.managedProperty().bind(fileInputConfig.visibleProperty());
        final TextArea logInputText = new TextArea();
        HBox.setHgrow(logInputText, Priority.ALWAYS);
        final int numLinesForEnteringLogInputManually = 10;
        logInputText.setPrefRowCount(numLinesForEnteringLogInputManually);
        logInputText.setStyle("-fx-font-family: monospace");
        this.enteredLogTextProperty.bind(logInputText.textProperty());
        final HBox enterTextConfig = new HBox();
        enterTextConfig.getChildren().add(logInputText);
        enterTextConfig.visibleProperty().bind(inputTypeText.selectedProperty());
        enterTextConfig.managedProperty().bind(enterTextConfig.visibleProperty());
        lines.getChildren().addAll(inputTypeLine, fileInputConfig, enterTextConfig);
        formItems.add(Triple.of("Trace Log", lines, null));
    }

    private void insertConfigItemsIntoGrid(final GridPane gp, final List<Triple<String, Node, String>> formItems)
    {
        for (int i = 0; i < formItems.size(); i++)
        {
            final String title = formItems.get(i).getLeft();
            final Node inputElement = formItems.get(i).getMiddle();
            final String description = formItems.get(i).getRight();

            // Put text flow object into cell. If a Text instance is used only, it will grab the whole cell size and center the text
            // (horizontally and vertically). Therefore, the table cell alignment does not work.
            final TextFlow titleText = new TextFlow(new Text(title));
            titleText.setStyle("-fx-font-weight: bold;");
            final TextFlow fieldName = new TextFlow(titleText);
            fieldName.autosize();
            fieldName.setMinWidth(fieldName.getWidth());
            gp.add(fieldName, 0, i);
            final VBox vbox = new VBox(inputElement);
            if (StringUtils.isNotBlank(description))
            {
                vbox.getChildren().add(new TextFlow(new Text(description)));
            }
            gp.add(vbox, 1, i);
        }
    }

    private Job createJobFromConfig()
    {
        GenericLogReader logReader;
        if (TestRunEditor.this.loadLogFromEnteredTextProperty.get())
        {
            logReader = new GenericLogReader(
                new BufferedReader(new StringReader(TestRunEditor.this.enteredLogTextProperty.get())));
        }
        else
        {
            Validate.isTrue(TestRunEditor.this.loadLogFromFileProperty.get());
            logReader = new GenericLogReader(new File(TestRunEditor.this.logFilePathProperty.get()));
        }

        if (StringUtils.isNotBlank(TestRunEditor.this.logReaderConfigurationFilePathProperty.get()))
        {
            try
            {
                logReader.overwriteCurrentSettingsWithSettingsInConfigurationFile(new JSONObject(
                    IOUtils.toString(new File(TestRunEditor.this.logReaderConfigurationFilePathProperty.get()).toURI(),
                        LogReaderEditor.LOG_READER_CONFIG_ENCODING)));
            }
            catch (JSONException | IOException e)
            {
                throw new RuntimeException("Error while loading the log reader configuration.", e);
            }
        }

        final JsonAutomatonDefinition automaton = new JsonAutomatonDefinition(
            new File(TestRunEditor.this.automatonFilePathProperty.get()));

        final LogSink newSink = new LogSink();
        newSink.setAutomaton(automaton);
        newSink.setParameters(this.parseParameters());

        final Job job = new Job();
        job.setLogReader(logReader);
        job.setSinks(Arrays.asList(newSink));
        return job;
    }

    private Map<String, String> parseParameters()
    {
        final Pattern variable = Pattern.compile(AutomatonParameters.PARAMETER_PREG);
        final Map<String, String> params = new HashMap<>();
        final String[] lines = StringUtils.split(TestRunEditor.this.parametersProperty.get(), "\r\n");
        for (final String line : lines)
        {
            if (StringUtils.isNotBlank(line))
            {
                final Matcher m = variable.matcher(line);
                if (m.find())
                {
                    final String key = m.group(1);
                    if (line.charAt(m.end()) == '=')
                    {
                        final String value = line.substring(m.end() + 1);
                        params.put(key, value);
                    }
                }
            }
        }
        return params;
    }

    @Override
    protected void createNewDocument()
    {
    }

    @Override
    protected void loadDocumentFromFileAndAssignToFile(final File srcFile)
    {
    }

    @Override
    protected void saveDocument()
    {
    }
}
