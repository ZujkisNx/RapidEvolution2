package com.mixshare.rapid_evolution.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ColumnResizer {
    
    private static int buffer = 2;
    private static int maxColumnResize = 400;
    
    public static void adjustColumnPreferredWidths(JTable table) {
        TableColumnModel columnModel = table.getColumnModel();
        for (int col = 0; col < table.getColumnCount(); ++col) {
            int maxwidth = 0;
            for (int row = 0; row < table.getRowCount(); ++row) {
                TableCellRenderer rend = table.getCellRenderer(row, col);
                Object value = table.getValueAt(row, col);
                Component comp = rend.getTableCellRendererComponent(table, value, false, false, row, col);
                maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
            }
            TableColumn column = columnModel.getColumn(col);
            
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Object headerValue = column.getHeaderValue();
            Component headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, col);
            maxwidth = Math.max(maxwidth, headerComp.getPreferredSize().width);
            maxwidth = Math.min(maxwidth + buffer, maxColumnResize);
            column.setPreferredWidth(maxwidth);
        }
        
    }

}
