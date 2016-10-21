/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2016 Pixida GmbH
 */

package de.pixida.logtest.designer.automaton;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javafx.scene.Node;

class PropertyNode extends RectangularNode
{
    static final String WITHOUT_TITLE = null;

    private final Connector connector;
    private final BaseNode parent;

    PropertyNode(final Graph graph, final BaseNode aParent, final String title, final int zIndexNode, final int zIndexEdge)
    {
        super(graph, ContentDisplayMode.ADJUST_RECT, zIndexNode);

        this.setTitle(title);

        this.parent = aParent;

        this.connector = new Connector(this.getGraph(), zIndexEdge);
        this.connector.setDashed();
        this.connector.hideArrow();
        this.connector.targetIsDependentNode();
        this.connector.attach(this.parent, this);
        this.connector.realign();

        this.hideAndRemoveFromScene();
    }

    String apply(String value)
    {
        if (StringUtils.isBlank(value))
        {
            value = null;
        }
        if (value != null)
        {
            if (!this.isVisible())
            {
                final double margin = 80.0;
                this.moveTo(this.parent.getPosition().add(margin, margin));
                this.showAndAddToScene();
            }

            this.setContent(value);
        }
        else
        {
            this.hideAndRemoveFromScene();
        }
        return value;
    }

    Boolean apply(final Boolean value)
    {
        final boolean val = BooleanUtils.toBoolean(value);
        if (val)
        {
            if (!this.isVisible())
            {
                final double margin = 80.0;
                this.moveTo(this.parent.getPosition().add(margin, margin));
                this.showAndAddToScene();
            }
        }
        else
        {
            this.hideAndRemoveFromScene();
        }
        return value;
    }

    static void saveAllPropertyNodesToJson(final Map<String, PropertyNode> mapPropertyNameToPropertyNode, final JSONObject designerConfig)
    {
        for (final Entry<String, PropertyNode> propertyNode : mapPropertyNameToPropertyNode.entrySet())
        {
            if (propertyNode.getValue().isVisible())
            {
                final JSONObject nodeConfig = new JSONObject();
                propertyNode.getValue().saveDimensionsToJson(nodeConfig);
                designerConfig.put(propertyNode.getKey(), nodeConfig);
            }
        }
    }

    static void loadAllPropertyNodesFromJson(final Map<String, PropertyNode> mapPropertyNameToPropertyNode, final JSONObject designerConfig)
    {
        for (final Entry<String, PropertyNode> propertyNode : mapPropertyNameToPropertyNode.entrySet())
        {
            final JSONObject nodeConfig = designerConfig.optJSONObject(propertyNode.getKey());
            if (propertyNode.getValue().isVisible() && nodeConfig != null)
            {
                propertyNode.getValue().loadDimensionsFromJson(nodeConfig);
            }
        }
    }

    @Override
    Node getConfigFrame()
    {
        return this.parent.getConfigFrame();
    }

    private void hideAndRemoveFromScene()
    {
        // We can not only use hide() because the object will remain inside the scene graph and e.g. resize the pane. We still use hide()
        // to be able to call isVisible().
        this.hide();
        this.connector.hide();
        this.getGraph().removeObject(this);
        this.getGraph().removeObject(this.connector);
    }

    private void showAndAddToScene()
    {
        // We can not only use show() because the object will remain inside the scene graph and e.g. resize the pane. We still use show()
        // to be able to call isVisible().
        this.show();
        this.connector.show();
        this.getGraph().addObject(this);
        this.getGraph().addObject(this.connector);
    }
}
