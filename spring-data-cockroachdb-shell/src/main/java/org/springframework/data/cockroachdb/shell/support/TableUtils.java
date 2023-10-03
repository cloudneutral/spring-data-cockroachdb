package org.springframework.data.cockroachdb.shell.support;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

public abstract class TableUtils {
    private TableUtils() {
    }

    public static String prettyPrintResultSet(ResultSet resultSet) throws SQLException {
        List<Object> headers = new ArrayList<>();
        List<List<Object>> data = new ArrayList<>();

        ResultSetMetaData metaData = resultSet.getMetaData();
        final int columnCount = metaData.getColumnCount();
        for (int col = 1; col <= columnCount; col++) {
            headers.add(metaData.getColumnName(col));
        }

        data.add(headers);

        while (resultSet.next()) {
            List<Object> row = new ArrayList<>();
            for (int col = 1; col <= columnCount; col++) {
                row.add(resultSet.getObject(col));
            }
            data.add(row);
        }

        TableModel model = new TableModel() {
            @Override
            public int getRowCount() {
                return data.size();
            }

            @Override
            public int getColumnCount() {
                return columnCount;
            }

            @Override
            public Object getValue(int row, int column) {
                List<Object> rowData = data.get(row);
                return rowData.get(column);
            }
        };

        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addInnerBorder(BorderStyle.fancy_light);
        tableBuilder.addHeaderBorder(BorderStyle.fancy_double);
//        tableBuilder.on(CellMatchers.column(0)).addSizer(new AbsoluteWidthSizeConstraints(20));
        return tableBuilder.build().render(120);
    }
}
