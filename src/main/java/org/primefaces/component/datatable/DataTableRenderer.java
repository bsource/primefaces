/*
 * Copyright 2009-2011 Prime Technology.
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
package org.primefaces.component.datatable;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;
import java.util.Locale;
import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;
import org.primefaces.component.celleditor.CellEditor;
import org.primefaces.component.column.Column;
import org.primefaces.component.columngroup.ColumnGroup;
import org.primefaces.component.columns.Columns;
import org.primefaces.component.row.Row;
import org.primefaces.component.subtable.SubTable;
import org.primefaces.component.summaryrow.SummaryRow;
import org.primefaces.model.SortOrder;
import org.primefaces.renderkit.CoreRenderer;
import org.primefaces.util.ComponentUtils;

public class DataTableRenderer extends CoreRenderer {

    protected DataHelper dataHelper;

    public DataTableRenderer() {
        dataHelper = new DataHelper();
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        DataTable table = (DataTable) component;
        boolean isSortRequest = table.isSortRequest(context);

        if(table.isFilteringEnabled()) {
            dataHelper.decodeFilters(context, table);
            
            if(!isSortRequest && table.getValueExpression("sortBy") != null && !table.isLazy()) {
                sort(context, table);
            }
        }

        if(table.isSelectionEnabled()) {
            dataHelper.decodeSelection(context, table);
        }

        if(table.isPaginationRequest(context)) {
            dataHelper.decodePageRequest(context, table);
        } 
        else if(isSortRequest) {
            dataHelper.decodeSortRequest(context, table);
        }

        decodeBehaviors(context, component);

        if(table.isPaginator()) {
            updatePaginationMetadata(context, table);
        }
    }
    
    @Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException{
		DataTable table = (DataTable) component;

        if(table.isBodyUpdate(context)) {
            encodeTbody(context, table);
        } 
        else if(table.isRowExpansionRequest(context)) {
            encodeRowExpansion(context, table);
        }
        else if(table.isRowEditRequest(context)) {
            encodeEditedRow(context, table);
        }
        else if(table.isScrollingRequest(context)) {
            encodeLiveRows(context, table);
        }
        else {
            encodeMarkup(context, table);
            encodeScript(context, table);
        }
	}
	
	protected void encodeScript(FacesContext context, DataTable table) throws IOException{
		ResponseWriter writer = context.getResponseWriter();
		String clientId = table.getClientId(context);

		writer.startElement("script", table);
		writer.writeAttribute("type", "text/javascript", null);
        
        writer.write("$(function() {");

        writer.write(table.resolveWidgetVar() + " = new PrimeFaces.widget.DataTable('" + clientId + "',{");

        //Connection
        UIComponent form = ComponentUtils.findParentForm(context, table);
        if(form == null) {
            throw new FacesException("DataTable : \"" + clientId + "\" must be inside a form element.");
        }
        writer.write("formId:'" + form.getClientId(context) + "'");

        //Pagination
        if(table.isPaginator()) {
            encodePaginatorConfig(context, table);
        }

        //Selection
        if(table.isRowSelectionEnabled()) {
            encodeSelectionConfig(context, table);
        }

        if(table.isColumnSelectionEnabled()) {
            writer.write(",columnSelectionMode:'" + table.getColumnSelectionMode() + "'");
        }

        //Row expansion
        if(table.getRowExpansion() != null) {
            writer.write(",expansion:true");
            if(table.getOnExpandStart() != null) {
                writer.write(",onExpandStart:function(row) {" + table.getOnExpandStart() + "}");
            }
        }

        //Scrolling
        if(table.isScrollable()) {
            writer.write(",scrollable:true");
            writer.write(",liveScroll:" + table.isLiveScroll());
            writer.write(",scrollStep:" + table.getScrollRows());
            writer.write(",scrollLimit:" + table.getRowCount());
        }

        //Resizable Columns
        if(table.isResizableColumns()) {
            writer.write(",resizableColumns:true");
        }

        //Behaviors
        encodeClientBehaviors(context, table);

        writer.write("});});");

		writer.endElement("script");
	}

	protected void encodeMarkup(FacesContext context, DataTable table) throws IOException{
		ResponseWriter writer = context.getResponseWriter();
		String clientId = table.getClientId(context);
        boolean scrollable = table.isScrollable();

        //style
        String containerClass = scrollable ? DataTable.CONTAINER_CLASS + " " + DataTable.SCROLLABLE_CONTAINER_CLASS : DataTable.CONTAINER_CLASS;
        containerClass = table.getStyleClass() != null ? containerClass + " " + table.getStyleClass() : containerClass;
        String style = null;
        
        if(table.isResizableColumns()) {
            containerClass = containerClass + " " + DataTable.RESIZABLE_CONTAINER_CLASS; 
        }
        
        //paginator
        boolean hasPaginator = table.isPaginator();
        String paginatorPosition = table.getPaginatorPosition();
        
        //default sort
        if(!isPostBack() && table.getValueExpression("sortBy") != null && !table.isLazy()) {
            sort(context, table);
        }

        if(hasPaginator) {
            table.calculatePage();
        }

        writer.startElement("div", table);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("class", containerClass, "styleClass");
        if((style = table.getStyle()) != null) {
            writer.writeAttribute("style", style, "style");
        }

        encodeFacet(context, table, table.getHeader(), DataTable.HEADER_CLASS);

        if(hasPaginator && !paginatorPosition.equalsIgnoreCase("bottom")) {
            encodePaginatorMarkup(context, table, "top");
        }

        if(scrollable) {
            encodeScrollableTable(context, table);

        } else {
            encodeRegularTable(context, table);
        }

        if(hasPaginator && !paginatorPosition.equalsIgnoreCase("top")) {
            encodePaginatorMarkup(context, table, "bottom");
        }
        
        encodeFacet(context, table, table.getFooter(), DataTable.FOOTER_CLASS);

        if(table.isSelectionEnabled()) {
            encodeSelectionHolder(context, table);
        }

        writer.endElement("div");
	}

    protected void encodeRegularTable(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.startElement("table", null);
        encodeThead(context, table);
        encodeTFoot(context, table);
        encodeTbody(context, table);
        writer.endElement("table");
    }

    protected void encodeScrollableTable(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        int scrollHeight = table.getScrollHeight();
        int scrollWidth = table.getScrollWidth();
        boolean hasScrollHeight = scrollHeight != Integer.MIN_VALUE;
        boolean hasScrollWidth = scrollWidth != Integer.MIN_VALUE;
        StringBuilder style = new StringBuilder();
        
        if(scrollHeight != Integer.MIN_VALUE)
            style.append("height:").append(scrollHeight).append("px;");
        if(hasScrollWidth)
            style.append("width:").append(scrollWidth).append("px;");
        
        //header
        writer.startElement("div", null);
        writer.writeAttribute("class", DataTable.SCROLLABLE_HEADER_CLASS, null);
        if(hasScrollWidth) {
            writer.writeAttribute("style", "width:" + scrollWidth + "px", null);
        }
        
        writer.startElement("div", null);
        writer.writeAttribute("class", DataTable.SCROLLABLE_HEADER_BOX_CLASS, null);
        
        writer.startElement("table", null);
        encodeThead(context, table);
        writer.endElement("table");
        
        writer.endElement("div");
        writer.endElement("div");

        //body
        writer.startElement("div", null);
        writer.writeAttribute("class", DataTable.SCROLLABLE_BODY_CLASS, null);
        if(style.length() > 0) {
            writer.writeAttribute("style", style, null);
        }
        writer.startElement("table", null);
        encodeTbody(context, table);
        writer.endElement("table");
        writer.endElement("div");

        //footer
        writer.startElement("div", null);
        writer.writeAttribute("class", DataTable.SCROLLABLE_FOOTER_CLASS, null);
        if(hasScrollWidth) {
            writer.writeAttribute("style", "width:" + scrollWidth + "px", null);
        }
        
        writer.startElement("div", null);
        writer.writeAttribute("class", DataTable.SCROLLABLE_FOOTER_BOX_CLASS, null);
        
        writer.startElement("table", null);
        encodeTFoot(context, table);
        writer.endElement("table");
        
        writer.endElement("div");
        
        writer.endElement("div");
    }

    protected void encodeColumnHeader(FacesContext context, DataTable table, Column column) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = column.getClientId(context);
        ValueExpression tableSortByVe = table.getValueExpression("sortBy");
        ValueExpression columnSortByVe = column.getValueExpression("sortBy");
        boolean isSortable = columnSortByVe != null;
        boolean hasFilter = column.getValueExpression("filterBy") != null;
        String selectionMode = column.getSelectionMode();
        String sortIcon = DataTable.SORTABLE_COLUMN_ICON_CLASS;
        
        String columnClass = isSortable ? DataTable.COLUMN_HEADER_CLASS + " " + DataTable.SORTABLE_COLUMN_CLASS : DataTable.COLUMN_HEADER_CLASS;
        columnClass = hasFilter ? columnClass + " " + DataTable.FILTER_COLUMN_CLASS : columnClass;
        columnClass = selectionMode != null ? columnClass + " " + DataTable.SELECTION_COLUMN_CLASS : columnClass;

        if(isSortable) {
            String columnSortByExpression = columnSortByVe.getExpressionString();
            
            if(tableSortByVe != null) {
                String tableSortByExpression = tableSortByVe.getExpressionString();

                if(tableSortByExpression != null && tableSortByExpression.equals(columnSortByExpression)) {
                    String sortOrder = table.getSortOrder().toUpperCase();

                    if(sortOrder.equals("ASCENDING"))
                        sortIcon = DataTable.SORTABLE_COLUMN_ASCENDING_ICON_CLASS;
                    else if(sortOrder.equals("DESCENDING"))
                        sortIcon = DataTable.SORTABLE_COLUMN_DESCENDING_ICON_CLASS;

                    columnClass = columnClass + " ui-state-active";
                }
            }
        }
        
        writer.startElement("th", null);
        writer.writeAttribute("id", clientId, null);
        writer.writeAttribute("class", columnClass, null);
        
        if(column.getRowspan() != 1) writer.writeAttribute("rowspan", column.getRowspan(), null);
        if(column.getColspan() != 1) writer.writeAttribute("colspan", column.getColspan(), null);
        
        //column content wrapper
        String style = column.getStyle();
        String styleClass = column.getStyleClass();
        styleClass = styleClass == null ? DataTable.COLUMN_CONTENT_WRAPPER : DataTable.COLUMN_CONTENT_WRAPPER + " " + styleClass;
        
        writer.startElement("div", null);
        writer.writeAttribute("class", styleClass, null);
        if(style != null) {
            writer.writeAttribute("style", style, null);
        }

        if(selectionMode != null && selectionMode.equalsIgnoreCase("multiple")) {
            writer.startElement("input", null);
            writer.writeAttribute("type", "checkbox", null);
            writer.writeAttribute("name", clientId + "_checkAll", null);
            writer.writeAttribute("onclick", table.resolveWidgetVar() + ".toggleCheckAll(this)", null);
            writer.endElement("input");
        }
        else {
            if(hasFilter) {
                table.enableFiltering();
                String filterPosition = column.getFilterPosition();
                
                if(filterPosition.equals("bottom")) {
                    encodeColumnHeaderContent(context, column, sortIcon);
                    encodeFilter(context, table, column);
                }
                else if(filterPosition.equals("top")) {
                    encodeFilter(context, table, column);
                    encodeColumnHeaderContent(context, column, sortIcon);
                } 
                else {
                    throw new FacesException(filterPosition + " is an invalid option for filterPosition, valid values are 'bottom' or 'top'.");
                }
            }
            else {
                encodeColumnHeaderContent(context, column, sortIcon);
            }
        }
        
        writer.endElement("div");

        writer.endElement("th");
    }
    
    protected void encodeColumnHeaderContent(FacesContext context, Column column, String sortIcon) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        
        if(sortIcon != null) {
            writer.startElement("span", null);
            writer.writeAttribute("class", sortIcon, null);
            writer.endElement("span");
        }
                
        UIComponent header = column.getFacet("header");
        String headerText = column.getHeaderText();
        
        writer.startElement("span", null);
        
        if(header != null)
            header.encodeAll(context);
        else if(headerText != null)
            writer.write(headerText);
        
        writer.endElement("span");
    }

    protected void encodeColumnsHeader(FacesContext context, DataTable table, Columns columns) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String columnVar = columns.getVar();

        for(Object column : (Collection) columns.getValue()) {
            context.getExternalContext().getRequestMap().put(columnVar, column);
            UIComponent header = columns.getFacet("header");

            writer.startElement("th", null);
            writer.writeAttribute("class", DataTable.COLUMN_HEADER_CLASS, null);
            
            writer.startElement("div", null);
            writer.writeAttribute("class", DataTable.COLUMN_CONTENT_WRAPPER, null);
            
            if(header != null) {
                header.encodeAll(context);
            }
            
            writer.endElement("div");

            writer.endElement("th");
        }

        context.getExternalContext().getRequestMap().remove(columnVar);
    }

    protected void encodeFilter(FacesContext context, DataTable table, Column column) throws IOException {
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
        ResponseWriter writer = context.getResponseWriter();

        String widgetVar = table.resolveWidgetVar(); 
        String filterId = column.getClientId(context) + "_filter";
        String filterFunction = widgetVar + ".filter()";
        String filterStyleClass = column.getFilterStyleClass();
        filterStyleClass = filterStyleClass == null ? DataTable.COLUMN_FILTER_CLASS : DataTable.COLUMN_FILTER_CLASS + " " + filterStyleClass;

        if(column.getValueExpression("filterOptions") == null) {
            String filterEvent = "on" + column.getFilterEvent();
            String filterValue = params.containsKey(filterId) ? params.get(filterId) : "";

            writer.startElement("input", null);
            writer.writeAttribute("id", filterId, null);
            writer.writeAttribute("name", filterId, null);
            writer.writeAttribute("class", filterStyleClass, null);
            writer.writeAttribute("value", filterValue , null);
            writer.writeAttribute(filterEvent, filterFunction , null);

            if(column.getFilterStyle() != null) {
                writer.writeAttribute("style", column.getFilterStyle(), null);
            }

            writer.endElement("input");
            
        }
        else {
            writer.startElement("select", null);
            writer.writeAttribute("id", filterId, null);
            writer.writeAttribute("name", filterId, null);
            writer.writeAttribute("class", filterStyleClass, null);
            writer.writeAttribute("onchange", filterFunction, null);

            SelectItem[] itemsArray = (SelectItem[]) getFilterOptions(column);

            for(SelectItem item : itemsArray) {
                writer.startElement("option", null);
                writer.writeAttribute("value", item.getValue(), null);
                writer.write(item.getLabel());
                writer.endElement("option");
            }

            writer.endElement("select");
        }
        
    }

    protected SelectItem[] getFilterOptions(Column column) {
        Object options = column.getFilterOptions();
        
        if(options instanceof SelectItem[]) {
            return (SelectItem[]) options;
        } else if(options instanceof Collection<?>) {
            return ((Collection<SelectItem>) column.getFilterOptions()).toArray(new SelectItem[] {});
        } else {
            throw new FacesException("Filter options for column " + column.getClientId() + " should be a SelectItem array or collection");
        }
    }

    protected void encodeColumnFooter(FacesContext context, DataTable table, Column column) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        
        String style = column.getStyle();
        String styleClass = column.getStyleClass();
        styleClass = styleClass == null ? DataTable.COLUMN_CONTENT_WRAPPER : DataTable.COLUMN_CONTENT_WRAPPER + " " + styleClass;

        writer.startElement("td", null);
        writer.writeAttribute("class", DataTable.COLUMN_FOOTER_CLASS, null);
        if(column.getRowspan() != 1) writer.writeAttribute("rowspan", column.getRowspan(), null);
        if(column.getColspan() != 1) writer.writeAttribute("colspan", column.getColspan(), null);

        writer.startElement("div", null);
        writer.writeAttribute("class", styleClass, null);
        
        if(style != null) 
            writer.writeAttribute("style", style, null);
        
        //Footer content
        UIComponent facet = column.getFacet("footer");
        String text = column.getFooterText();
        if(facet != null) {
            facet.encodeAll(context);
        } else if(text != null) {
            writer.write(text);
        }
        
        writer.endElement("div");

        writer.endElement("td");
    }

    /**
     * Render column headers either in single row or nested if a columnGroup is defined
     */
    protected void encodeThead(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        ColumnGroup group = table.getColumnGroup("header");

        writer.startElement("thead", null);

        if(group != null && group.isRendered()) {

            for(UIComponent child : group.getChildren()) {
                if(child.isRendered() && child instanceof Row) {
                    Row headerRow = (Row) child;

                    writer.startElement("tr", null);

                    for(UIComponent headerRowChild : headerRow.getChildren()) {
                        if(headerRowChild.isRendered() && headerRowChild instanceof Column) {
                            encodeColumnHeader(context, table, (Column) headerRowChild);
                        }
                    }

                    writer.endElement("tr");
                }
            }

        } else {
            
            writer.startElement("tr", null);

            for(UIComponent kid : table.getChildren()) {
                if(kid.isRendered()) {
                    if(kid instanceof Column) {
                        encodeColumnHeader(context, table, (Column) kid);
                    }
                    else if(kid instanceof Columns) {
                        encodeColumnsHeader(context, table, (Columns) kid);
                    }
                }
            }

            writer.endElement("tr");
        }

        writer.endElement("thead");
    }

    protected void encodeTbody(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String rowIndexVar = table.getRowIndexVar();
        String clientId = table.getClientId(context);
        String emptyMessage = table.getEmptyMessage();
        SubTable subTable = table.getSubTable();
        SummaryRow summaryRow = table.getSummaryRow();
        
        if(table.isLazy()) {
            table.loadLazyData();
        }
        
        if(table.isSelectionEnabled()) {
            table.findSelectedRowKeys();
        }
        
        int rows = table.getRows();
		int first = table.getFirst();
        int rowCount = table.getRowCount();
        int rowCountToRender = rows == 0 ? (table.isLiveScroll() ? table.getScrollRows() : rowCount) : rows;
        boolean hasData = rowCount > 0;

        String tbodyClass = hasData ? DataTable.DATA_CLASS : DataTable.EMPTY_DATA_CLASS;
      
        writer.startElement("tbody", null);
        writer.writeAttribute("id", clientId + "_data", null);
        writer.writeAttribute("class", tbodyClass, null);

        if(hasData) {
            for(int i = first; i < (first + rowCountToRender); i++) {
                if(subTable != null) {
                    encodeSubTable(context, table, subTable, i, rowIndexVar);
                }
                else {

                    if(summaryRow != null && i != first && !dataHelper.isInSameGroup(context, table, i)) {
                        MethodExpression me = summaryRow.getListener();
                        if(me != null) {
                            me.invoke(context.getELContext(), new Object[]{table.getSortBy()});
                        }
                        summaryRow.encodeAll(context);
                    }
                    
                    encodeRow(context, table, clientId, i, rowIndexVar);
                }
            }
        }
        else if(emptyMessage != null){
            //Empty message
            writer.startElement("tr", null);
            writer.writeAttribute("class", DataTable.ROW_CLASS, null);

            writer.startElement("td", null);
            writer.writeAttribute("colspan", table.getColumns().size(), null);
            writer.startElement("div", null);
            writer.writeAttribute("class", DataTable.COLUMN_CONTENT_WRAPPER, null);
            writer.write(emptyMessage);
            writer.endElement("div");
            writer.endElement("td");
            
            writer.endElement("tr");
        }
		
        writer.endElement("tbody");

		//Cleanup
		table.setRowIndex(-1);
		if(rowIndexVar != null) {
			context.getExternalContext().getRequestMap().remove(rowIndexVar);
		}
    }

    protected void encodeRow(FacesContext context, DataTable table, String clientId, int rowIndex, String rowIndexVar) throws IOException {
        table.setRowIndex(rowIndex);
        if(!table.isRowAvailable()) {
            return;
        }

        //Row index var
        if(rowIndexVar != null) {
            context.getExternalContext().getRequestMap().put(rowIndexVar, rowIndex);
        }
        
        Object rowKey = null;
        if(table.isSelectionEnabled()) {
            //try rowKey attribute
            rowKey = table.getRowKey();
            
            //ask selectable datamodel
            if(rowKey == null)
                rowKey = table.getRowKeyFromModel(table.getRowData());
        }
        
        String rowMeta = clientId + "_r_" + rowIndex;
        rowMeta = rowKey == null ? rowMeta : rowMeta + "_" + rowKey;

        //Preselection
        boolean selected = table.getSelectedRowKeys().contains(rowKey);

        ResponseWriter writer = context.getResponseWriter();

        String userRowStyleClass = table.getRowStyleClass();
        String rowStyleClass = rowIndex % 2 == 0 ? DataTable.ROW_CLASS + " " + DataTable.EVEN_ROW_CLASS : DataTable.ROW_CLASS + " " + DataTable.ODD_ROW_CLASS;
        
        if(selected) {
            rowStyleClass = rowStyleClass + " ui-state-highlight";
        }

        if(userRowStyleClass != null) {
            rowStyleClass = rowStyleClass + " " + userRowStyleClass;
        }

        writer.startElement("tr", null);
        writer.writeAttribute("id", rowMeta, null);
        writer.writeAttribute("class", rowStyleClass, null);

        for(UIComponent kid : table.getChildren()) {
            if(kid.isRendered()) {
                if(kid instanceof Column) {
                    encodeRegularCell(context, table, (Column) kid, clientId, selected);
                }
                else if(kid instanceof Columns) {
                    encodeDynamicCell(context, table, (Columns) kid);
                }
            }
        }

        writer.endElement("tr");
        
        //used for summaryRow if any
        table.setPreviousRowData(table.getRowData());
    }

    protected void encodeRegularCell(FacesContext context, DataTable table, Column column, String clientId, boolean selected) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String style = column.getStyle();
        String styleClass = column.getStyleClass();
        styleClass = styleClass == null ? DataTable.COLUMN_CONTENT_WRAPPER : DataTable.COLUMN_CONTENT_WRAPPER + " " + styleClass;

        writer.startElement("td", null);

        if(column.getSelectionMode() != null) {
            writer.writeAttribute("class", DataTable.SELECTION_COLUMN_CLASS , null);
            
            writer.startElement("div", null);
            writer.writeAttribute("class", styleClass, null);
            
            if(style != null) {
                writer.writeAttribute("style", style, null);
            }

            encodeColumnSelection(context, table, clientId, column, selected);
            
            writer.endElement("div");
        }
        else {
            CellEditor editor = column.getCellEditor();
            if(editor != null) {
                writer.writeAttribute("class", DataTable.EDITABLE_COLUMN_CLASS , null);
            }

            writer.startElement("div", null);
            writer.writeAttribute("class", styleClass, null);
            
            if(style != null) {
                writer.writeAttribute("style", style, null);
            }
            
            column.encodeAll(context);
            writer.endElement("div");
        }

        writer.endElement("td");
    }

    protected void encodeDynamicCell(FacesContext context, DataTable table, Columns columns) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String columnVar = columns.getVar();
        String columnIndexVar = columns.getColumnIndexVar();
        int colIndex = 0;

        for(Object column : (Collection) columns.getValue()) {
            context.getExternalContext().getRequestMap().put(columnVar, column);
            context.getExternalContext().getRequestMap().put(columnIndexVar, colIndex);
            UIComponent header = columns.getFacet("header");

            writer.startElement("td", null);
            writer.startElement("div", null);
            writer.writeAttribute("class", DataTable.COLUMN_CONTENT_WRAPPER, null);
            columns.encodeAll(context);
            writer.endElement("div");
            writer.endElement("td");

            colIndex++;
        }

        context.getExternalContext().getRequestMap().remove(columnVar);
        context.getExternalContext().getRequestMap().remove(columnIndexVar);
    }

    protected void encodeTFoot(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        ColumnGroup group = table.getColumnGroup("footer");
        boolean shouldRender = table.hasFooterColumn() || (group != null && group.isRendered());

        if(!shouldRender)
            return;

        writer.startElement("tfoot", null);

        if(group != null) {

            for(UIComponent child : group.getChildren()) {
                if(child.isRendered() && child instanceof Row) {
                    Row footerRow = (Row) child;

                    writer.startElement("tr", null);

                    for(UIComponent footerRowChild : footerRow.getChildren()) {
                        if(footerRowChild.isRendered() && footerRowChild instanceof Column) {
                            encodeColumnFooter(context, table, (Column) footerRowChild);
                        }
                    }

                    writer.endElement("tr");
                }
            }

        }
        else {
            writer.startElement("tr", null);

            for(Column column : table.getColumns()) {
                encodeColumnFooter(context, table, column);
            }

            writer.endElement("tr");
        }
        
        writer.endElement("tfoot");
    }

    protected void encodeFacet(FacesContext context, DataTable table, UIComponent facet, String styleClass) throws IOException {
        if(facet == null)
            return;
        
        ResponseWriter writer = context.getResponseWriter();

        writer.startElement("div", null);
        writer.writeAttribute("class", styleClass, null);

        facet.encodeAll(context);

        writer.endElement("div");
    }

    protected void encodePaginatorConfig(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = table.getClientId(context);
        String paginatorPosition = table.getPaginatorPosition();
        String paginatorContainers = null;
        if(paginatorPosition.equalsIgnoreCase("both"))
            paginatorContainers = "'" + clientId + "_paginatortop','" + clientId + "_paginatorbottom'";
        else
            paginatorContainers = "'" + clientId + "_paginator" + paginatorPosition + "'";

        writer.write(",paginator:new YAHOO.widget.Paginator({");
        writer.write("rowsPerPage:" + table.getRows());
        writer.write(",totalRecords:" + table.getRowCount());
        writer.write(",initialPage:" + table.getPage());
        writer.write(",containers:[" + paginatorContainers + "]");

        if(table.getPageLinks() != 10) writer.write(",pageLinks:" + table.getPageLinks());
        if(table.getPaginatorTemplate() != null) writer.write(",template:'" + table.getPaginatorTemplate() + "'");
        if(table.getRowsPerPageTemplate() != null) writer.write(",rowsPerPageOptions : [" + table.getRowsPerPageTemplate() + "]");
        if(table.getCurrentPageReportTemplate() != null)writer.write(",pageReportTemplate:'" + table.getCurrentPageReportTemplate() + "'");
        if(!table.isPaginatorAlwaysVisible()) writer.write(",alwaysVisible:false");

        writer.write("})");
    }

    protected void encodeSelectionConfig(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.write(",selectionMode:'" + table.getSelectionMode() + "'");

        if(table.isDblClickSelect()) {
            writer.write(",dblclickSelect:true");
        }
    }

    protected void encodePaginatorMarkup(FacesContext context, DataTable table, String position) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = table.getClientId(context);
        
        String styleClass = "ui-paginator ui-paginator-" + position + " ui-widget-header";

        if(!position.equals("top") && table.getFooter() == null)
            styleClass = styleClass + " ui-corner-bl ui-corner-br";
        else if(!position.equals("bottom") && table.getHeader() == null)
            styleClass = styleClass + " ui-corner-tl ui-corner-tr";
        
        writer.startElement("div", null);
        writer.writeAttribute("id", clientId + "_paginator" + position, null);
        writer.writeAttribute("class", styleClass, null);
        writer.endElement("div");
    }

    protected void encodeSelectionHolder(FacesContext context, DataTable table) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
        String id = table.getClientId(context) + "_selection";

		writer.startElement("input", null);
		writer.writeAttribute("type", "hidden", null);
		writer.writeAttribute("id", id, null);
		writer.writeAttribute("name", id, null);
        writer.writeAttribute("value", table.getSelectedRowKeysAsString(), null);
		writer.endElement("input");
	}
	
    @Override
	public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
		//Rendering happens on encodeEnd
	}

    @Override
	public boolean getRendersChildren() {
		return true;
	}

    protected void encodeRowEditor(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String widgetVar = table.resolveWidgetVar();

        writer.startElement("span", null);
        writer.writeAttribute("class", DataTable.ROW_EDITOR_CLASS, null);

        writer.startElement("span", null);
        writer.writeAttribute("class", "ui-icon ui-icon-pencil", null);
        writer.endElement("span");

        writer.startElement("span", null);
        writer.writeAttribute("class", "ui-icon ui-icon-check", null);
        writer.writeAttribute("style", "display:none", null);
        writer.endElement("span");

        writer.startElement("span", null);
        writer.writeAttribute("class", "ui-icon ui-icon-close", null);
        writer.writeAttribute("style", "display:none", null);
        writer.endElement("span");

        writer.endElement("span");
    }

    protected void encodeRowExpansion(FacesContext context, DataTable table) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
        int expandedRowIndex = Integer.parseInt(params.get(table.getClientId(context) + "_expandedRowIndex"));

        table.setRowIndex(expandedRowIndex);

        writer.startElement("tr", null);
        writer.writeAttribute("style", "display:none", null);
        writer.writeAttribute("class", DataTable.EXPANDED_ROW_CONTENT_CLASS + " ui-widget-content", null);

        writer.startElement("td", null);
        writer.writeAttribute("colspan", table.getColumns().size(), null);

        table.getRowExpansion().encodeAll(context);

        writer.endElement("td");

        writer.endElement("tr");

        table.setRowIndex(-1);
    }

    protected void encodeColumnSelection(FacesContext context, DataTable table, String clientId, Column column, boolean selected) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String selectionMode = column.getSelectionMode();
        String name = clientId + "_selection";

        if(selectionMode.equalsIgnoreCase("single")) {
            writer.startElement("input", null);
            writer.writeAttribute("type", "radio", null);
            writer.writeAttribute("name", name + "_radio", null);
            if(selected) {
                writer.writeAttribute("checked", "checked", null);
            }
            writer.endElement("input");
            
        } else if(selectionMode.equalsIgnoreCase("multiple")) {
            writer.startElement("input", null);
            writer.writeAttribute("type", "checkbox", null);
            writer.writeAttribute("name", name + "_checkbox", null);
            if(selected) {
                writer.writeAttribute("checked", "checked", null);
            }
            writer.endElement("input");

        } else {
            throw new FacesException("Invalid column selection mode:" + selectionMode);
        }

    }

    protected void encodeEditedRow(FacesContext context, DataTable table) throws IOException {
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
        int editedRowId = Integer.parseInt(params.get(table.getClientId(context) + "_editedRowIndex"));

        table.setRowIndex(editedRowId);

        encodeRow(context, table, table.getClientId(context), editedRowId, table.getRowIndexVar());
    }

    protected void encodeLiveRows(FacesContext context, DataTable table) throws IOException {
        Map<String,String> params = context.getExternalContext().getRequestParameterMap();
        int scrollOffset = Integer.parseInt(params.get(table.getClientId(context) + "_scrollOffset"));
        String clientId = table.getClientId(context);
        String rowIndexVar = table.getRowIndexVar();

        for(int i = scrollOffset; i < (scrollOffset + table.getScrollRows()); i++) {
            encodeRow(context, table, clientId, i, rowIndexVar);
        }
    }
    
    protected void updatePaginationMetadata(FacesContext context, DataTable table) {
        ELContext elContext = context.getELContext();
        ValueExpression firstVe = table.getValueExpression("first");
        ValueExpression rowsVe = table.getValueExpression("rows");
        ValueExpression pageVE = table.getValueExpression("page");

        if(firstVe != null && !firstVe.isReadOnly(elContext))
            firstVe.setValue(context.getELContext(), table.getFirst());
        if(rowsVe != null && !rowsVe.isReadOnly(elContext))
            rowsVe.setValue(context.getELContext(), table.getRows());
        if(pageVE != null && !pageVE.isReadOnly(elContext))
            pageVE.setValue(context.getELContext(), table.getPage());
    }

    protected void sort(FacesContext context, DataTable table) {
        dataHelper.sort(context, table, table.getValueExpression("sortBy"), table.getVar(), SortOrder.valueOf(table.getSortOrder().toUpperCase(Locale.ENGLISH)), null);
    }

    protected void encodeSubTable(FacesContext context, DataTable table, SubTable subTable, int rowIndex, String rowIndexVar) throws IOException {
        table.setRowIndex(rowIndex);
        if(!table.isRowAvailable()) {
            return;
        }

        //Row index var
        if(rowIndexVar != null) {
            context.getExternalContext().getRequestMap().put(rowIndexVar, rowIndex);
        }
        
        subTable.encodeAll(context);
        
        if(rowIndexVar != null) {
			context.getExternalContext().getRequestMap().remove(rowIndexVar);
		}
    }
}