/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer;

import java.io.File;
import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;

import de.pixida.logtest.designer.automaton.AutomatonEditor;
import de.pixida.logtest.designer.logreader.LogReaderEditor;
import de.pixida.logtest.designer.testrun.TestRunEditor;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public abstract class Editor extends BorderPane
{
    public enum Type
    {
        AUTOMATON("Automaton", parentWindow -> new AutomatonEditor(parentWindow), "palette", true),
        LOG_READER_CONFIG("Log Reader", parentWindow -> new LogReaderEditor(parentWindow), "script", true),
        TEST_RUN("Test Run", parentWindow -> new TestRunEditor(parentWindow), "lightning", false);

        private String name;
        private String iconName;
        private Function<IMainWindow, Editor> factory;
        private final SimpleBooleanProperty supportsFiles = new SimpleBooleanProperty();

        private Type(final String aName, final Function<IMainWindow, Editor> aFactory, final String aIconName, final boolean aSupportsFiles)
        {
            this.name = aName;
            this.factory = aFactory;
            this.iconName = aIconName;
            this.supportsFiles.set(aSupportsFiles);
        }

        public String getName()
        {
            return this.name;
        }

        public String getIconName()
        {
            return this.iconName;
        }

        SimpleBooleanProperty supportsFilesProperty()
        {
            return this.supportsFiles;
        }

        public Editor createEditor(final IMainWindow value)
        {
            return this.factory.apply(value);
        }
    }

    private static int newDocumentRunningIndex = 1;

    private final Type type;
    private final IMainWindow mainWindow;
    private final StringExpression documentTitleLong;
    private final StringExpression documentTitleShort;
    private final SimpleStringProperty documentNameLong = new SimpleStringProperty();
    private final SimpleStringProperty documentNameShort = new SimpleStringProperty();
    private final SimpleStringProperty changedMarkerText = new SimpleStringProperty("");
    private boolean changed;
    private String fileDescription;
    private String fileMask;
    private File file;

    public Editor(final Type aType, final IMainWindow aMainWindow)
    {
        Validate.notNull(aType);
        Validate.notNull(aMainWindow);
        this.type = aType;
        this.mainWindow = aMainWindow;
        this.documentTitleLong = Bindings.concat(this.documentNameLong, this.changedMarkerText);
        this.documentTitleShort = Bindings.concat(this.documentNameShort, this.changedMarkerText);
        this.setChanged(false);
        this.updateDocumentName();
    }

    StringExpression getTitleLong()
    {
        return this.documentTitleLong;
    }

    StringExpression getTitleShort()
    {
        return this.documentTitleShort;
    }

    SimpleBooleanProperty supportsFilesProperty()
    {
        return this.type.supportsFilesProperty();
    }

    void setExtensionFiltersForFiles(final ObservableList<ExtensionFilter> extensionFilters)
    {
        extensionFilters.clear(); // To be clean...
        extensionFilters.add(new FileChooser.ExtensionFilter(this.fileDescription, this.fileMask));
    }

    boolean hasUnsavedChanges()
    {
        return this.changed;
    }

    File getDirectoryOfAssignedFile()
    {
        Validate.notNull(this.file);
        return this.file.getParentFile();
    }

    boolean isCurrentDocumentAssignedToFile()
    {
        return this.file != null;
    }

    boolean isCurrentDocumentAssignedToFile(final File value)
    {
        Validate.notNull(value);
        if (!this.isCurrentDocumentAssignedToFile())
        {
            return false;
        }
        Validate.notNull(this.file);
        return this.file.equals(this.file);
    }

    protected void setFileMaskAndDescription(final String aFileMask, final String aFileDescription)
    {
        this.fileMask = aFileMask;
        this.fileDescription = aFileDescription;
    }

    protected void setDocumentName(final String longName, final String shortName)
    {
        this.documentNameLong.set(longName);
        this.documentNameShort.set(shortName);
    }

    protected String getTypeName()
    {
        return this.type.getName();
    }

    public String getIconName()
    {
        return this.type.getIconName();
    }

    protected IMainWindow getMainWindow()
    {
        return this.mainWindow;
    }

    protected void setChanged(final boolean value)
    {
        if (value != this.changed) // Method might been called very often
        {
            this.changed = value;
            this.changedMarkerText.set(this.changed ? "*" : "");
        }
    }

    protected void assignDocumentToFile(final File value)
    {
        Validate.notNull(value);
        this.file = value;
        this.updateDocumentName();
    }

    protected File getFile()
    {
        return this.file;
    }

    protected abstract void init();

    protected abstract void createNewDocument();

    protected abstract void loadDocumentFromFileAndAssignToFile(File srcFile);

    protected abstract void saveDocument();

    private void updateDocumentName()
    {
        final String nameForUnassingedDocument = String.format("New %s [%d]", this.type.getName(), newDocumentRunningIndex++);
        this.setDocumentName(this.file != null ? this.file.getPath() : nameForUnassingedDocument,
            this.file != null ? FilenameUtils.removeExtension(FilenameUtils.getName(this.file.getName())) : nameForUnassingedDocument);
    }
}
