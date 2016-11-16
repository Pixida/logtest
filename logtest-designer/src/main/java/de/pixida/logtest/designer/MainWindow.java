/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import de.pixida.logtest.designer.Editor.Type;
import de.pixida.logtest.designer.commons.ExceptionDialog;
import de.pixida.logtest.designer.commons.Icons;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

class MainWindow implements IMainWindow
{
    private static final String APP_ICON = "weather_sun";
    private static final String APP_TITLE = "Logtest Designer";

    private final Stage primaryStage;

    private final BorderPane root = new BorderPane();

    private final TabPane tabPane = new TabPane();

    private MenuItem menuItemSave;
    private MenuItem menuItemSaveAs;

    MainWindow(final Stage aPrimaryStage)
    {
        this.primaryStage = aPrimaryStage;
        this.initWindow();
    }

    private void bindEditorDependentProperties()
    {
        StringExpression titlePropertyBinding;
        BooleanProperty fileHandlingEnabled;
        final Editor currentEditor = this.getCurrentEditor();
        if (currentEditor == null)
        {
            titlePropertyBinding = Bindings.concat(APP_TITLE);
            fileHandlingEnabled = new SimpleBooleanProperty(false);
        }
        else
        {
            titlePropertyBinding = Bindings.concat(currentEditor.getTypeName() + " - ").concat(currentEditor.getTitleLong())
                .concat(" - " + APP_TITLE);
            fileHandlingEnabled = currentEditor.supportsFilesProperty();
        }
        this.primaryStage.titleProperty().bind(titlePropertyBinding);
        this.menuItemSave.disableProperty().bind(fileHandlingEnabled.not());
        this.menuItemSaveAs.disableProperty().bind(fileHandlingEnabled.not());
    }

    private void initWindow()
    {
        final VBox top = new VBox();
        top.getChildren().addAll(this.createMenuBar());
        this.root.setTop(top);

        this.bindEditorDependentProperties();

        this.tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            this.bindEditorDependentProperties();
        });
        this.root.setCenter(this.tabPane);

        this.primaryStage.getIcons().add(Icons.getIconImage(APP_ICON));

        final Scene rootScene = new Scene(this.root, 800, 600);
        this.primaryStage.setScene(rootScene);
        this.primaryStage.setOnCloseRequest(event -> {
            if (!this.handleExitApplication())
            {
                event.consume(); // Prevent window from closing
            }
        });
        this.primaryStage.show();
    }

    private Node createMenuBar()
    {
        final MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(this.createMenus());
        return menuBar;
    }

    private List<Menu> createMenus()
    {
        final Menu file = new Menu("File");
        this.createAndAppendFileMenuItems(file);

        final Menu help = new Menu("Help");
        this.createAndAppendHelpMenuItems(help);

        return Arrays.asList(file, help);
    }

    private void createAndAppendHelpMenuItems(final Menu help)
    {
        final MenuItem visitDocumentation = new MenuItem("Visit Online Documentation");
        visitDocumentation.setGraphic(Icons.getIconGraphics("help"));
        visitDocumentation.setOnAction(event -> {
            Exception ex = null;
            final String url = "https://github.com/Pixida/logtest/wiki";
            final Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE))
            {
                try
                {
                    desktop.browse(new URI(url));
                }
                catch (final Exception e)
                {
                    ex = e;
                }
            }
            else
            {
                ex = new Exception("Browsing not supported.");
            }
            if (ex != null)
            {
                ExceptionDialog.showFatalException("Failed to open browser", "Visit us at " + url, ex);
            }
        });

        final MenuItem about = new MenuItem("About " + APP_TITLE);
        about.setGraphic(Icons.getIconGraphics(APP_ICON));
        about.setOnAction(event -> {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initStyle(StageStyle.UTILITY);
            alert.setTitle(about.getText());
            alert.setHeaderText(APP_TITLE);
            alert.setContentText("Copyright (C) " + Calendar.getInstance().get(Calendar.YEAR) + " Pixida GmbH\n"
                + "\n"
                + "This application includes FAMFAMFAM icons (http://www.famfamfam.com).");
            alert.showAndWait();
        });

        help.getItems().addAll(visitDocumentation, about);
    }

    private void createAndAppendFileMenuItems(final Menu menu)
    {
        final Menu newDocument = new Menu("New");
        final Menu open = new Menu("Open");
        for (final Type type : Editor.Type.values())
        {
            final MenuItem newFile = new MenuItem(type.getName());
            newFile.setOnAction(event -> this.handleCreateNewDocument(type));
            newDocument.getItems().add(newFile);

            if (type.supportsFilesProperty().get())
            {
                final MenuItem openFile = new MenuItem(type.getName());
                openFile.setOnAction(event -> this.handleLoadDocument(type));
                open.getItems().add(openFile);
            }
        }

        this.menuItemSave = new MenuItem("Save");
        this.menuItemSave.setGraphic(Icons.getIconGraphics("disk"));
        this.menuItemSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        this.menuItemSave.setOnAction(event -> this.handleSaveDocument());

        this.menuItemSaveAs = new MenuItem("Save as");
        this.menuItemSaveAs.setOnAction(event -> this.handleSaveDocumentAs());

        final MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(event -> this.handleExitApplication());

        menu.getItems().addAll(newDocument, open, this.menuItemSave, this.menuItemSaveAs, new SeparatorMenuItem(), exit);
    }

    private void handleLoadDocument(final Type type)
    {
        final Editor newEditor = this.createNewEditorByTypeAndInitializeIt(type);
        final FileChooser fileChooser = this.createFileDialog(newEditor, "Open");
        final File selectedFile = fileChooser.showOpenDialog(this.primaryStage);
        if (selectedFile != null)
        {
            final Tab openedDocument = this.findTabThatIsAssignedToFile(selectedFile);
            if (openedDocument != null)
            {
                this.tabPane.getSelectionModel().select(openedDocument);
            }
            else
            {
                try
                {
                    newEditor.loadDocumentFromFileAndAssignToFile(selectedFile);
                }
                catch (final RuntimeException re)
                {
                    ExceptionDialog.showFatalException("Failed to load " + newEditor.getTypeName() + ".",
                        "An error occurred while loading the " + newEditor.getTypeName() + ".", re);
                    return;
                }
                this.addEditorAsNewTab(newEditor);
            }
        }
    }

    private FileChooser createFileDialog(final Editor newEditor, final String actionName)
    {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(actionName + " " + newEditor.getTypeName());
        newEditor.setExtensionFiltersForFiles(fileChooser.getExtensionFilters());
        final Editor currentEditor = this.getCurrentEditor();
        if (currentEditor != null && currentEditor.isDocumentAssignedToFile())
        {
            fileChooser.setInitialDirectory(currentEditor.getDirectoryOfAssignedFile());
        }
        return fileChooser;
    }

    private void handleCreateNewDocument(final Type type)
    {
        final Editor newEditor = this.createNewEditorByTypeAndInitializeIt(type);
        try
        {
            newEditor.createNewDocument();
        }
        catch (final RuntimeException re)
        {
            ExceptionDialog.showFatalException("Failed to create new " + newEditor.getTypeName() + ".",
                "An error occurred while creating a new " + newEditor.getTypeName() + ".", re);
        }
        this.addEditorAsNewTab(newEditor);
    }

    private Editor createNewEditorByTypeAndInitializeIt(final Type type)
    {
        final Editor newEditor = type.createEditor(this);
        Validate.notNull(newEditor);
        newEditor.init();
        return newEditor;
    }

    private void addEditorAsNewTab(final Editor editor)
    {
        final Tab newTab = new Tab();
        newTab.setContent(editor);
        newTab.setGraphic(Icons.getIconGraphics(editor.getIconName()));
        newTab.textProperty().bind(editor.getTitleShort());
        newTab.setOnCloseRequest(event -> {
            if (!this.handleCloseTab())
            {
                event.consume(); // Do not continue / leave tab open
            }
        });
        this.tabPane.getTabs().add(newTab);
        this.tabPane.getSelectionModel().select(newTab);
    }

    private boolean saveDocument()
    {
        final Editor currentEditor = this.getCurrentEditor();
        try
        {
            currentEditor.saveDocument();
            return true;
        }
        catch (final RuntimeException re)
        {
            ExceptionDialog.showFatalException("Failed to save " + currentEditor.getTypeName() + ".",
                "An error occurred while saving the " + currentEditor.getTypeName() + ".", re);
            return false;
        }
    }

    private boolean handleSaveDocument()
    {
        if (!this.getCurrentEditor().isDocumentAssignedToFile())
        {
            return this.handleSaveDocumentAs();
        }
        return this.saveDocument();
    }

    private boolean handleSaveDocumentAs()
    {
        final FileChooser fileChooser = this.createFileDialog(this.getCurrentEditor(), "Save");
        final File selectedFile = fileChooser.showSaveDialog(this.primaryStage);
        if (selectedFile == null)
        {
            return false;
        }
        else
        {
            boolean overwriteIfPresent = true;
            final Tab openedDocument = this.findTabThatIsAssignedToFile(selectedFile);
            if (openedDocument != null)
            {
                final Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Confirm overwrite");
                alert.setHeaderText("The file is already opened.");
                alert.setContentText("Do you want to overwrite the file?");
                final Optional<ButtonType> choice = alert.showAndWait();
                if (choice.get() != ButtonType.OK)
                {
                    overwriteIfPresent = false;
                }
            }
            if (overwriteIfPresent)
            {
                this.getCurrentEditor().assignDocumentToFile(selectedFile);
                return this.saveDocument();
            }
            else
            {
                return false;
            }
        }
    }

    private Tab findTabThatIsAssignedToFile(final File file)
    {
        for (final Tab tab : this.tabPane.getTabs())
        {
            if (this.getEditorOfTab(tab).isDocumentAssignedToFile(file))
            {
                return tab;
            }
        }
        return null;
    }

    private boolean handleExitApplication()
    {
        final ObservableList<Tab> tabs = this.tabPane.getTabs();
        for (final Tab tab : tabs)
        {
            this.tabPane.getSelectionModel().select(tab);
            if (!this.handleCloseTab())
            {
                return false;
            }
        }

        Platform.exit();
        return true;
    }

    private boolean handleCloseTab()
    {
        final Editor currentEditor = this.getCurrentEditor();
        if (!currentEditor.hasUnsavedChanges())
        {
            return true;
        }

        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Confirm close");
        alert.setHeaderText("If the " + currentEditor.getTypeName() + " is not saved, all changes are lost.");
        alert.setContentText("Do you want to save the " + currentEditor.getTypeName() + "?");

        final ButtonType yes = new ButtonType("Yes");
        final ButtonType no = new ButtonType("No");
        final ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yes, no, cancel);

        final Optional<ButtonType> choice = alert.showAndWait();
        if (choice.get() == yes)
        {
            // Continue with saving
            if (!this.handleSaveDocument())
            {
                return false;
            }
        }
        else if (choice.get() == no)
        {
            // Dispose document
        }
        else
        {
            // User closed dialog or chose cancel
            return false;
        }

        return true;
    }

    private Editor getCurrentEditor()
    {
        final Tab currentTab = this.tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null)
        {
            // Nothing selected
            return null;
        }
        final Editor currentEditor = this.getEditorOfTab(currentTab);
        return currentEditor;
    }

    private Editor getEditorOfTab(final Tab currentTab)
    {
        Validate.notNull(currentTab.getContent());
        final Node currentContent = currentTab.getContent();
        if (currentContent instanceof Editor)
        {
            return (Editor) currentContent;
        }
        return null;
    }
}
