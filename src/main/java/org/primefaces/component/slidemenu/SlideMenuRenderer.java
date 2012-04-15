/*
 * Copyright 2009-2012 Prime Teknoloji.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.primefaces.component.slidemenu;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.component.menu.BaseMenuRenderer;
import org.primefaces.component.menu.Menu;

public class SlideMenuRenderer extends BaseMenuRenderer {

    protected void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
        SlideMenu menu = (SlideMenu) abstractMenu;
		String clientId = menu.getClientId(context);
		String widgetVar = menu.resolveWidgetVar();
        String position = menu.getPosition();

		startScript(writer, clientId);
        
        writer.write("$(function() {");
        
        writer.write("PrimeFaces.cw('SlideMenu','" + widgetVar + "',{");
        writer.write("id:'" + clientId + "'");
        writer.write(",position:'" + position + "'");
        
        if(menu.getEasing() != null) {
            writer.write(",easing:'" + menu.getEasing() + "'");
        }
        
        //dynamic position
        if(position.equalsIgnoreCase("dynamic")) {
           writer.write(",my:'" + menu.getMy() + "'");
           writer.write(",at:'" + menu.getAt() + "'");

            UIComponent trigger = menu.findComponent(menu.getTrigger());
            String triggerClientId = trigger == null ? menu.getTrigger() : trigger.getClientId(context);
            
            writer.write(",trigger:'" + triggerClientId + "'");
            writer.write(",triggerEvent:'" + menu.getTriggerEvent() + "'");
        }

        writer.write("});});");

		endScript(writer);
	}

	protected void encodeMarkup(FacesContext context, AbstractMenu abstractMenu) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
        SlideMenu menu = (SlideMenu) abstractMenu;
		String clientId = menu.getClientId(context);
        boolean dynamic = menu.getPosition().equals("dynamic");
        
        String style = menu.getStyle();
        String styleClass = menu.getStyleClass();
        String defaultStyleClass = dynamic ? Menu.DYNAMIC_CONTAINER_CLASS : Menu.STATIC_CONTAINER_CLASS;
        defaultStyleClass = defaultStyleClass + " " + Menu.MENU_SLIDING_CLASS;
        styleClass = styleClass == null ? defaultStyleClass : defaultStyleClass+ " " + styleClass;
        
        writer.startElement("div", menu);
		writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("class", styleClass, "styleClass");
        if(style != null) {
            writer.writeAttribute("style", style, "style");
        }
        writer.writeAttribute("role", "menu", null);
        
		writer.startElement("ul", null);
        writer.writeAttribute("class", Menu.LIST_CLASS, null);

        encodeTieredMenuContent(context, menu);

		writer.endElement("ul");
        
        //back navigator
        writer.startElement("div", menu);
        writer.writeAttribute("class", Menu.BACKWARD_CLASS, null);
        writer.startElement("span", menu);
        writer.writeAttribute("class", Menu.BACKWARD_ICON_CLASS, null);
        writer.endElement("span");
        writer.write(menu.getBackLabel());
        writer.endElement("div");
        
        writer.endElement("div");
	}
}