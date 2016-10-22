/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.logreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONException;
import org.json.JSONObject;

import de.pixida.logtest.designer.Editor;
import de.pixida.logtest.designer.IMainWindow;
import de.pixida.logtest.designer.commons.ExceptionDialog;
import de.pixida.logtest.designer.commons.Icons;
import de.pixida.logtest.designer.commons.SelectFileButton;
import de.pixida.logtest.logreaders.GenericLogReader;
import de.pixida.logtest.logreaders.GenericLogReader.HandlingOfNonHeadlineLines;
import de.pixida.logtest.logreaders.ILogEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;

public class LogReaderEditor extends Editor
{
    public static final Charset LOG_READER_CONFIG_ENCODING = StandardCharsets.UTF_8;

    public static final String LOG_FILE_ICON_NAME = "page_white_text";

    public static final String LOG_READER_FILE_MASK = "*.json";
    public static final String LOG_READER_FILE_DESCRIPTION = "Log Reader Configuration (" + LOG_READER_FILE_MASK + ")";

    private GenericLogReader logReader;
    private TableView<LogEntryTableRow> parsedLogEntries = new TableView<>();
    private final ObservableList<LogEntryTableRow> parsedLogEntryItems = FXCollections
        .observableList(new ArrayList<LogEntryTableRow>());

    public static class LogEntryTableRow
    {
        private final ILogEntry logEntry;
        private final StringProperty lineNumber;
        private final StringProperty time;
        private final StringProperty channel;
        private final StringProperty payload;

        LogEntryTableRow(final ILogEntry aLogEntry)
        {
            this.logEntry = aLogEntry;
            this.lineNumber = new SimpleStringProperty(this, "lineNumber", String.valueOf(this.logEntry.getLineNumber()));
            this.time = new SimpleStringProperty(this, "time", String.valueOf(this.logEntry.getTime()));
            this.channel = new SimpleStringProperty(this, "channel", StringUtils.defaultIfEmpty(this.logEntry.getChannel(), "[default]"));
            this.payload = new SimpleStringProperty(this, "payload", this.logEntry.getPayload());
        }

        public StringProperty lineNumberProperty()
        {
            return this.lineNumber;
        }

        public StringProperty timeProperty()
        {
            return this.time;
        }

        public StringProperty channelProperty()
        {
            return this.channel;
        }

        public StringProperty payloadProperty()
        {
            return this.payload;
        }
    }

    public LogReaderEditor(final IMainWindow mainWindow)
    {
        super(Editor.Type.LOG_READER_CONFIG, mainWindow);

        this.parsedLogEntries = new TableView<>(this.parsedLogEntryItems);
        this.parsedLogEntries.setPlaceholder(new Text("No log entries to display."));
        final TableColumn<LogEntryTableRow, String> lineNoCol = new TableColumn<LogEntryTableRow, String>("LineNo");
        lineNoCol.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        lineNoCol.setSortable(false);
        lineNoCol.setGraphic(Icons.getIconGraphics("key"));
        final TableColumn<LogEntryTableRow, String> timeCol = new TableColumn<LogEntryTableRow, String>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setSortable(false);
        timeCol.setGraphic(Icons.getIconGraphics("clock"));
        final TableColumn<LogEntryTableRow, String> channelCol = new TableColumn<LogEntryTableRow, String>("Channel");
        channelCol.setCellValueFactory(new PropertyValueFactory<>("channel"));
        channelCol.setSortable(false);
        channelCol.setGraphic(Icons.getIconGraphics("connect"));
        final TableColumn<LogEntryTableRow, String> payloadCol = new TableColumn<LogEntryTableRow, String>("Payload");
        payloadCol.setCellValueFactory(new PropertyValueFactory<>("payload"));
        payloadCol.setSortable(false);
        payloadCol.setGraphic(Icons.getIconGraphics("page_white_text_width"));
        this.parsedLogEntries.getColumns().add(lineNoCol);
        this.parsedLogEntries.getColumns().add(timeCol);
        this.parsedLogEntries.getColumns().add(channelCol);
        this.parsedLogEntries.getColumns().add(payloadCol);

        this.setFileMaskAndDescription(LOG_READER_FILE_MASK, LOG_READER_FILE_DESCRIPTION);
    }

    @Override
    protected void init()
    {
    }

    private void createDialogItems()
    {
        Validate.notNull(this.logReader); // Will be used to initialize input field values

        // CHECKSTYLE:OFF Yes, we are using lots of constants here. It does not make sense to name them using final variables.
        final GridPane gp = new GridPane();
        gp.setAlignment(Pos.BASELINE_LEFT);
        gp.setHgap(10d);
        gp.setVgap(15d);
        gp.setPadding(new Insets(5d));
        final ColumnConstraints column1 = new ColumnConstraints();
        final ColumnConstraints column2 = new ColumnConstraints();
        column1.setHgrow(Priority.NEVER);
        column2.setHgrow(Priority.SOMETIMES);
        gp.getColumnConstraints().addAll(column1, column2);
        this.insertConfigItemsIntoGrid(gp, this.createConfigurationForm());
        final TitledPane configPane = new TitledPane("Edit Configuration", gp);
        configPane.setGraphic(Icons.getIconGraphics("pencil"));
        configPane.setCollapsible(false);

        final VBox lines = this.createRunForm();
        final TitledPane testPane = new TitledPane("Test Configuration", lines);
        testPane.setGraphic(Icons.getIconGraphics("script_go"));
        testPane.setCollapsible(false);

        final VBox panes = new VBox(configPane, testPane);
        panes.setSpacing(10d);
        final ScrollPane sp = new ScrollPane(panes);
        sp.setPadding(new Insets(10d));
        sp.setFitToWidth(true);
        this.setCenter(sp);
        // CHECKSTYLE:ON
    }

    public VBox createRunForm()
    {
        // CHECKSTYLE:OFF Yes, we are using lots of constants here. It does not make sense to name them using final variables.
        final VBox lines = new VBox();
        lines.setSpacing(10d);
        final HBox inputTypeLine = new HBox();
        inputTypeLine.setSpacing(30d);
        final ToggleGroup group = new ToggleGroup();
        final RadioButton inputTypeText = new RadioButton("Paste/Enter text");
        inputTypeText.setToggleGroup(group);
        final RadioButton inputTypeFile = new RadioButton("Read log file");
        inputTypeFile.setToggleGroup(group);
        inputTypeLine.getChildren().add(inputTypeText);
        inputTypeLine.getChildren().add(inputTypeFile);
        inputTypeText.setSelected(true);
        final TextField pathInput = new TextField();
        HBox.setHgrow(pathInput, Priority.ALWAYS);
        final Button selectLogFileButton = SelectFileButton.createButtonWithFileSelection(pathInput, LOG_FILE_ICON_NAME, "Select log file", null, null);
        final Text pathInputLabel = new Text("Log file path: ");
        final HBox fileInputConfig = new HBox();
        fileInputConfig.setAlignment(Pos.CENTER_LEFT);
        fileInputConfig.visibleProperty().bind(inputTypeFile.selectedProperty());
        fileInputConfig.managedProperty().bind(fileInputConfig.visibleProperty());
        fileInputConfig.getChildren().addAll(pathInputLabel, pathInput, selectLogFileButton);
        final TextArea logInputText = new TextArea();
        HBox.setHgrow(logInputText, Priority.ALWAYS);
        logInputText.setPrefRowCount(10);
        logInputText.setStyle("-fx-font-family: monospace");
        final HBox enterTextConfig = new HBox();
        enterTextConfig.getChildren().add(logInputText);
        enterTextConfig.visibleProperty().bind(inputTypeText.selectedProperty());
        enterTextConfig.managedProperty().bind(enterTextConfig.visibleProperty());
        final Button startBtn = new Button("Read Log");
        startBtn.setPadding(new Insets(8d));
        // CHECKSTYLE:ON
        startBtn.setGraphic(Icons.getIconGraphics("control_play_blue"));
        HBox.setHgrow(startBtn, Priority.ALWAYS);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(event -> this.runLogFileReader(inputTypeFile, pathInput, logInputText));
        final HBox startLine = new HBox();
        startLine.getChildren().add(startBtn);
        lines.getChildren().addAll(inputTypeLine, fileInputConfig, enterTextConfig, startLine, new Text("Results:"), this.parsedLogEntries);
        return lines;
    }

    public void runLogFileReader(final RadioButton inputTypeFile, final TextField pathInput, final TextArea logInputText)
    {
        this.parsedLogEntryItems.clear();

        try
        {
            GenericLogReader reader;
            if (inputTypeFile.isSelected())
            {
                reader = new GenericLogReader(new File(pathInput.getText()));
            }
            else
            {
                reader = new GenericLogReader(new BufferedReader(new StringReader(logInputText.getText())));
            }
            reader.overwriteCurrentSettingsWithSettingsInConfigurationFile(this.logReader.getSettingsForConfigurationFile());
            this.logReader = reader;
            ILogEntry nextLogEntry;
            while ((nextLogEntry = reader.getNextEntry()) != null)
            {
                this.parsedLogEntryItems.add(new LogEntryTableRow(nextLogEntry));
            }
        }
        catch (final RuntimeException re)
        {
            ExceptionDialog.showFatalException("Error reading log entries", "An error occurred while reading the log entries.", re);
        }
    }

    private List<Triple<String, Node, String>> createConfigurationForm()
    {
        final List<Triple<String, Node, String>> formItems = new ArrayList<>();

        // Headline pattern
        final TextField textInput = new TextField(this.logReader.getHeadlinePattern());
        textInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            this.logReader.setHeadlinePattern(newValue);
            this.setChanged(true);
        });
        textInput.setStyle("-fx-font-family: monospace");
        formItems
            .add(Triple.of("Headline Pattern", textInput, "The perl style regular expression is used to spot the beginning of"
                + " log entries in the log file. If a log entry consists of multiple lines, this pattern must only match the first"
                + " line, called \"head line\". Groups can intentionally be matched to spot values like timestamp or channel name."
                + " All matching groups are removed from the payload before they are processed by an automaton."));

        // Index of timestamp
        Supplier<Integer> getter = () -> this.logReader.getHeadlinePatternIndexOfTimestamp();
        Consumer<Integer> setter = value -> this.logReader.setHeadlinePatternIndexOfTimestamp(value);
        final TextField indexOfTimestampInput = this.createIntegerInputField(textInput, getter, setter);
        formItems.add(Triple.of("Timestamp Group", indexOfTimestampInput, "Denotes which matching group in the headline pattern contains"
            + " the timestamp. Index 0 references the whole pattern match, index 1 is the first matching group etc. The timestamp must"
            + " always be a valid integer. Currently, this integer is always interpreted as milliseconds. If no value is set, no"
            + " timestamp will be extracted and timing conditions cannot be used. If the referenced matching group is optional"
            + " and does not match for a specific head line, the last recent timestamp will be used for the extracted log entry."));

        // Index of channel
        getter = () -> this.logReader.getHeadlinePatternIndexOfChannel();
        setter = value -> this.logReader.setHeadlinePatternIndexOfChannel(value);
        final TextField indexOfChannelInput = this.createIntegerInputField(textInput, getter, setter);
        formItems.add(Triple.of("Channel Group", indexOfChannelInput, "Denotes which matching group in the headline pattern contains"
            + " the channel. If the value is empty or the matching group is optional and it did not match, the default channel is used"
            + " for the extracted log entry."));

        // Trim payload
        CheckBox cb = new CheckBox();
        cb.setSelected(this.logReader.getTrimPayload());
        cb.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            this.logReader.setTrimPayload(BooleanUtils.toBoolean(newValue));
            this.setChanged(true);
        });
        formItems.add(Triple.of("Trim Payload", cb, "Only has effect on multiline payloads."
            + " If enabled, all leading and trailing whitespaces are removed from the payload"
            + " after the matching groups are removed. This allows for regular expressions in the automaton that match the beginning"
            + " and the end of the payload, without having to take care too much for whitespaces in the source log file."));

        // Handling of non headline lines
        this.createInputForHandlingOfNonHeadlineLines(formItems);

        // Trim payload
        cb = new CheckBox();
        cb.setSelected(this.logReader.getRemoveEmptyPayloadLinesFromMultilineEntry());
        cb.selectedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            this.logReader.setRemoveEmptyPayloadLinesFromMultilineEntry(BooleanUtils.toBoolean(newValue));
            this.setChanged(true);
        });
        formItems.add(Triple.of("Remove Empty Lines", cb, "If enabled, empty lines will be removed from multiline payload entries."));

        // Charset
        final SortedMap<String, Charset> charsets = Charset.availableCharsets();
        final ChoiceBox<String> encodingInput = new ChoiceBox<>(FXCollections.observableArrayList(charsets.keySet()));
        encodingInput.getSelectionModel().select(this.logReader.getLogFileCharset().name());
        encodingInput.getSelectionModel().selectedItemProperty()
            .addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
                this.logReader.setLogFileCharset(charsets.get(newValue));
                this.setChanged(true);
            });
        formItems.add(Triple.of("Log File Encoding", encodingInput, "Encoding of the log file. Note that some of the encodings might"
            + " be platform specific such that reading the log on a different platform might fail. Usually, log files are written"
            + " using UTF-8, UTF-16, ISO-8859-1 or ASCII."));

        return formItems;
    }

    public void createInputForHandlingOfNonHeadlineLines(final List<Triple<String, Node, String>> formItems)
    {
        final Map<HandlingOfNonHeadlineLines, String> mapValueToChoice = new HashMap<>();
        mapValueToChoice.put(HandlingOfNonHeadlineLines.FAIL, "Abort - Each line in the log file is assumed to be a log entry");
        mapValueToChoice.put(HandlingOfNonHeadlineLines.CREATE_MULTILINE_ENTRY, "Append to payload - This will create multiline payloads");
        mapValueToChoice.put(HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP,
            "Create new log entry and use timestamp of recent log entry");
        mapValueToChoice.put(HandlingOfNonHeadlineLines.ASSUME_LAST_TIMESTAMP_AND_CHANNEL,
            "Create new log entry and use timestamp and channel of recent log entry");
        final ChoiceBox<HandlingOfNonHeadlineLines> handlingOfNonHeadlineLinesInput = new ChoiceBox<>(
            FXCollections.observableArrayList(HandlingOfNonHeadlineLines.values()));
        handlingOfNonHeadlineLinesInput.setConverter(new StringConverter<HandlingOfNonHeadlineLines>()
        {
            @Override
            public String toString(final HandlingOfNonHeadlineLines object)
            {
                return mapValueToChoice.get(object);
            }

            @Override
            public HandlingOfNonHeadlineLines fromString(final String string)
            {
                for (final Entry<HandlingOfNonHeadlineLines, String> entry : mapValueToChoice.entrySet())
                {
                    if (entry.getValue() == string) // Intentionally comparing references to obtain a bijection
                    {
                        return entry.getKey();
                    }
                }
                return null; // Should never happen
            }
        });
        handlingOfNonHeadlineLinesInput.getSelectionModel().select(this.logReader.getHandlingOfNonHeadlineLines());
        handlingOfNonHeadlineLinesInput.getSelectionModel().selectedIndexProperty()
            .addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                this.logReader.setHandlingOfNonHeadlineLines(handlingOfNonHeadlineLinesInput.getItems().get(newValue.intValue()));
                this.setChanged(true);
            });
        formItems.add(Triple.of("Dangling Lines", handlingOfNonHeadlineLinesInput,
            "Define what to do if dangling lines are spotted. Dangling lines are lines which do not match the headline pattern, i.e."
                + " which do not introduce a new log entry."));
    }

    private TextField createIntegerInputField(final TextField textInput, final Supplier<Integer> getter, final Consumer<Integer> setter)
    {
        final TextField integerInput = new TextField(getter.get() == null ? "" : String.valueOf(getter.get()));
        integerInput.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*"))
            {
                integerInput.setText(newValue.replaceAll("[^\\d]", ""));
                newValue = textInput.getText();
            }
            if (StringUtils.isNotBlank(newValue))
            {
                try
                {
                    setter.accept(Integer.parseInt(newValue));
                }
                catch (final NumberFormatException nfe)
                {
                    // This can only happen if the value is "too long" / too high for "int"
                    integerInput.setText(String.valueOf(Integer.MAX_VALUE));
                    setter.accept(Integer.MAX_VALUE);
                }
            }
            else
            {
                setter.accept(null);
            }
            this.setChanged(true);
        });
        final double maxWidthOfIntegerInput = 80d;
        integerInput.setMaxWidth(maxWidthOfIntegerInput);
        return integerInput;
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
            final Text descriptionText = new Text(description);
            final VBox vbox = new VBox(inputElement, new TextFlow(descriptionText));
            gp.add(vbox, 1, i);
        }
    }

    @Override
    protected void createNewDocument()
    {
        this.createLogReaderWithDummyInput();
        this.createDialogItems();
    }

    @Override
    protected void loadDocumentFromFileAndAssignToFile(final File srcFile)
    {
        this.createLogReaderWithDummyInput();
        try
        {
            this.logReader.overwriteCurrentSettingsWithSettingsInConfigurationFile(
                new JSONObject(IOUtils.toString(srcFile.toURI(), LOG_READER_CONFIG_ENCODING)));
        }
        catch (JSONException | IOException e)
        {
            throw new RuntimeException(e);
        }
        this.assignDocumentToFile(srcFile);
        this.createDialogItems();
    }

    @Override
    protected void saveDocument()
    {
        Validate.notNull(this.getFile());
        final int indentSpaces = 4;
        try
        {
            FileUtils.write(this.getFile(), this.logReader.getSettingsForConfigurationFile().toString(indentSpaces),
                LOG_READER_CONFIG_ENCODING);
        }
        catch (final JSONException e)
        {
            throw new RuntimeException("JSON format exception while writing file", e);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Error while saving file: " + e.getMessage());
        }
        this.setChanged(false);
    }

    private void createLogReaderWithDummyInput()
    {
        final BufferedReader br = new BufferedReader(new StringReader("")); // Dummy input
        this.logReader = new GenericLogReader(br);
    }
}
