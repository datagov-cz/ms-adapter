package cz.gov.data.ms.sharepoint;

import com.google.gson.JsonElement;
import com.microsoft.graph.models.ColumnDefinition;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.ListItem;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.ListRequestBuilder;
import com.microsoft.graph.serializer.AdditionalDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Sharepoint<R> {

    private static final Logger LOG = LoggerFactory.getLogger(Sharepoint.class);

    protected GraphServiceClient<R> graphServiceClient;

    public Sharepoint(GraphServiceClient<R> graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
    }

    public SharepointList downloadList(
            String siteIdentifier, String listIdentifier) {
        var listRequestBuilder = graphServiceClient
                .sites(siteIdentifier)
                .lists(listIdentifier);
        var columns = downloadColumns(listRequestBuilder);
        var rows = loadRows(listRequestBuilder, columns);
        return new SharepointList(columns, rows);
    }

    protected List<Column> downloadColumns(
            ListRequestBuilder listRequestBuilder) {
        var response = listRequestBuilder.columns().buildRequest().get();
        if (response == null) {
            return Collections.emptyList();
        }
        var page = response.getCurrentPage();
        var result = new ArrayList<Column>(page.size());
        while (true) {
            for (ColumnDefinition column : page) {
                result.add(createColumn(column));
            }
            if (response.getNextPage() == null) {
                // There is no next page.
                break;
            }
            var nextColumns = response.getNextPage().buildRequest().get();
            if (nextColumns == null) {
                // There is no next page.
                break;
            }
            page = nextColumns.getCurrentPage();
        }
        return result;
    }

    protected Column createColumn(ColumnDefinition column) {
        String name = column.name;
        String label = column.displayName;
        if (column.text != null) {
            return new Column(ColumnType.String, name, label);
        } else if (column.dateTime != null) {
            return new Column(ColumnType.DataTime, name, label);
        } else if (column.number != null) {
            return new Column(ColumnType.Number, name, label);
        } else if (column.choice != null) {
            return new Column(ColumnType.Choice, name, label);
        } else if (column.msgraphBoolean != null) {
            return new Column(ColumnType.Boolean, name, label);
        } else if (column.contentApprovalStatus != null) {
            return new Column(ColumnType.ContentApprovalStatus, name, label);
        } else if (column.hyperlinkOrPicture != null) {
            return new Column(ColumnType.HyperlinkOrPicture, name, label);
        } else if (column.lookup != null) {
            return new Column(ColumnType.LookUp, name, label);
        } else if (column.personOrGroup != null) {
            return new Column(ColumnType.PersonOrGroup, name, label);
        } else if (column.term != null) {
            return new Column(ColumnType.Term, name, label);
        } else if (column.thumbnail != null) {
            return new Column(ColumnType.Thumbnail, name, label);
        } else {
            return new Column(ColumnType.Unknown, name, label);
        }
    }

    protected List<Row> loadRows(
            ListRequestBuilder listRequestBuilder, List<Column> columns) {
        List<Option> options = List.of(createSelectOption(columns));
        var response = listRequestBuilder.items().buildRequest(options).get();
        if (response == null) {
            return Collections.emptyList();
        }
        var page = response.getCurrentPage();
        var result = new ArrayList<Row>(page.size());
        while (true) {
            for (ListItem row : page) {
                result.add(createRow(columns, row));
            }
            if (response.getNextPage() == null) {
                // There is no next page.
                break;
            }
            var nextColumns = response.getNextPage().buildRequest().get();
            if (nextColumns == null) {
                // There is no next page.
                break;
            }
            page = nextColumns.getCurrentPage();
        }
        return result;
    }

    protected QueryOption createSelectOption(List<Column> columns) {
        List<String> names = columns.stream().map(item -> item.name).toList();
        String value = "fields(select=" + String.join(",", names) + ")";
        return new QueryOption("expand", value);
    }

    protected Row createRow(List<Column> columns, ListItem item) {
        if (item.fields == null) {
            return new Row(Collections.emptyList());
        }
        var values = new ArrayList<Cell>(columns.size());
        AdditionalDataManager data = item.fields.additionalDataManager();
        for (Column column : columns) {
            JsonElement value = data.get(column.name);
            values.add(new Cell(column, value));
        }
        return new Row(values);
    }

    public List<SharepointFile> listDriveDirectory(
            String siteIdentifier, String drivePath)
            throws SharepointException {
        List<String> path = Arrays.asList(drivePath.split("/"));
        if (path.isEmpty()) {
            LOG.warn("Path is empty");
            return Collections.emptyList();
        }
        var pathIterator = path.iterator();
        String driveName = pathIterator.next();
        String driveIdentifier = findDriveByName(siteIdentifier, driveName);
        if (driveIdentifier == null) {
            LOG.warn("Can't find drive with required name '{}'.", driveName);
            return Collections.emptyList();
        }
        String directoryIdentifier = findDirectoryOnDrive(
                siteIdentifier, driveIdentifier, pathIterator);
        if (directoryIdentifier == null) {
            LOG.warn("Directory not found.");
            return Collections.emptyList();
        }
        return listDriveDirectoryByIdentifier(
                siteIdentifier, driveIdentifier, directoryIdentifier);
    }

    protected String findDriveByName(
            String siteIdentifier, String driveName)
            throws SharepointException {
        var drives = graphServiceClient
                .sites(siteIdentifier)
                .drives()
                .buildRequest().get();
        if (drives == null) {
            throw new SharepointException("Can't list drives!");
        }
        var drivesPage = drives.getCurrentPage();
        for (Drive item : drivesPage) {
            if (driveName.equals(item.name)) {
                return item.id;
            }
        }
        return null;
    }

    protected String findDirectoryOnDrive(
            String siteIdentifier, String driveIdentifier,
            Iterator<String> pathIterator) throws SharepointException {
        var driveBuilder = graphServiceClient
                .sites(siteIdentifier)
                .drives(driveIdentifier);
        var directoryItems = driveBuilder
                .root()
                .children()
                .buildRequest()
                .get();
        StringBuilder visitedPath = new StringBuilder();
        String resultIdentifier = null;
        while (pathIterator.hasNext()) {
            String nextName = pathIterator.next();
            if (directoryItems == null) {
                throw new SharepointException("Can't list drive content!");
            }
            String nextIdentifier = null;
            for (DriveItem driveItem : directoryItems.getCurrentPage()) {
                if (nextName.equals(driveItem.name)) {
                    nextIdentifier = driveItem.id;
                    break;
                }
            }
            if (nextIdentifier == null) {
                LOG.info("Can't find '{}' in '{}'.", nextName, visitedPath);
                return null;
            }
            // Move to next iteration.
            visitedPath.append('/').append(nextName);
            resultIdentifier = nextIdentifier;
            directoryItems = driveBuilder
                    .items(nextIdentifier)
                    .children()
                    .buildRequest()
                    .get();
        }
        return resultIdentifier;
    }

    protected List<SharepointFile> listDriveDirectoryByIdentifier(
            String siteIdentifier, String driveIdentifier,
            String directoryIdentifier) throws SharepointException {
        var items = graphServiceClient
                .sites(siteIdentifier)
                .drives(driveIdentifier)
                .items(directoryIdentifier)
                .children()
                .buildRequest()
                .get();
        if (items == null) {
            throw new SharepointException("Can't iterate directory!");
        }
        var page = items.getCurrentPage();
        return page.stream().map(item -> new SharepointFile(
                siteIdentifier, item.id, item.name)
        ).toList();
    }

    public void downloadFile(SharepointFile file, Path outputDirectory)
            throws IOException, SharepointException {
        String url = "/sites/" + file.siteIdentifier +
                "/drive/items/" + file.fileIdentifier +
                "/content";
        InputStream source = graphServiceClient
                .customRequest(url, InputStream.class)
                .buildRequest()
                .get();
        if (source == null) {
            LOG.error("Can't find file '{}' name '{}'.",
                    file.fileIdentifier, file.fileName);
            throw new SharepointException("Can't open required file.");
        }
        Path destination = outputDirectory.resolve(file.fileName);
        try (var output = Files.newOutputStream(destination)) {
            source.transferTo(output);
        } finally {
            source.close();
        }
    }

}
