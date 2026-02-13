package cz.gov.data.ms.sharepoint;

import com.microsoft.graph.models.ColumnDefinition;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.ListItem;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.sites.item.lists.item.ListItemRequestBuilder;
import com.microsoft.graph.sites.item.lists.item.items.ItemsRequestBuilder;
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

public class Sharepoint {

    private static final Logger LOG = LoggerFactory.getLogger(Sharepoint.class);

    protected GraphServiceClient graphServiceClient;

    public Sharepoint(GraphServiceClient graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
    }

    public SharepointList downloadList(
            String siteIdentifier, String listIdentifier) {
        var listRequestBuilder = graphServiceClient
                .sites().bySiteId(siteIdentifier)
                .lists().byListId(listIdentifier);
        var columns = downloadColumns(listRequestBuilder);
        var rows = loadRows(listRequestBuilder, columns);
        return new SharepointList(columns, rows);
    }

    protected List<Column> downloadColumns(
            ListItemRequestBuilder listRequestBuilder) {
        // We start with the first page.
        var response = listRequestBuilder.columns().get();
        if (response == null) {
            return Collections.emptyList();
        }
        var pages = response.getValue();
        var result = new ArrayList<Column>();
        while (pages != null) {
            // Process page data.
            for (ColumnDefinition column : pages) {
                result.add(createColumn(column));
            }
            // Check next page or end.
            if (response.getOdataNextLink() == null) {
                // There is no more pages, we can terminate.
                break;
            } else {
                // Move to the next page.
                response = listRequestBuilder
                        .withUrl(response.getOdataNextLink())
                        .columns().get();
                if (response == null) {
                    break;
                }
                pages = response.getValue();
            }
        }
        return result;
    }

    protected Column createColumn(ColumnDefinition column) {
        String name = column.getName();
        String label = column.getDisplayName();
        if (column.getText() != null) {
            return new Column(ColumnType.String, name, label);
        } else if (column.getDateTime() != null) {
            return new Column(ColumnType.DataTime, name, label);
        } else if (column.getNumber() != null) {
            return new Column(ColumnType.Number, name, label);
        } else if (column.getChoice() != null) {
            return new Column(ColumnType.Choice, name, label);
        } else if (column.getBoolean() != null) {
            return new Column(ColumnType.Boolean, name, label);
        } else if (column.getContentApprovalStatus() != null) {
            return new Column(ColumnType.ContentApprovalStatus, name, label);
        } else if (column.getHyperlinkOrPicture() != null) {
            return new Column(ColumnType.HyperlinkOrPicture, name, label);
        } else if (column.getLookup() != null) {
            return new Column(ColumnType.LookUp, name, label);
        } else if (column.getPersonOrGroup() != null) {
            return new Column(ColumnType.PersonOrGroup, name, label);
        } else if (column.getText() != null) {
            return new Column(ColumnType.Term, name, label);
        } else if (column.getThumbnail() != null) {
            return new Column(ColumnType.Thumbnail, name, label);
        } else {
            return new Column(ColumnType.Unknown, name, label);
        }
    }

    protected List<Row> loadRows(
            ListItemRequestBuilder listRequestBuilder, List<Column> columns) {
        var response = listRequestBuilder.items()
                .get(config -> prepareRequestConfig(columns, config));
        if (response == null) {
            return Collections.emptyList();
        }
        var pages = response.getValue();
        var result = new ArrayList<Row>();
        while (pages != null) {
            // Process page data.
            for (ListItem row : pages) {
                result.add(createRow(columns, row));
            }
            // Check next page or end.
            if (response.getOdataNextLink() == null) {
                // There is no more pages, we can terminate.
                break;
            } else {
                // Move to the next page.
                response = listRequestBuilder
                        .withUrl(response.getOdataNextLink())
                        .items()
                        .get(config -> prepareRequestConfig(columns, config));
                if (response == null) {
                    break;
                }
                pages = response.getValue();
            }
        }
        return result;
    }

    // https://learn.microsoft.com/en-us/graph/api/listitem-list?view=graph-rest-1.0&tabs=java#example-1-get-list-items-with-specific-fields
    protected void prepareRequestConfig(List<Column> columns, ItemsRequestBuilder.GetRequestConfiguration config) {
        assert config.queryParameters != null;
        config.queryParameters.expand = new String[]{"fields"};
    }

    protected Row createRow(List<Column> columns, ListItem item) {
        if (item.getFields() == null) {
            return new Row(Collections.emptyList());
        }
        var values = new ArrayList<Cell>(columns.size());
        var data = item.getFields().getAdditionalData();
        for (Column column : columns) {
            // Value from com.microsoft.kiota.serialization.
            var value = data.get(column.name);
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
        // Retrieve directory content
        var driverRoot = graphServiceClient.drives().byDriveId(driveIdentifier).root().get();
        if (driverRoot == null) {
            LOG.warn("There is no root for '{}'.", driveName);
            return Collections.emptyList();
        }
        String directoryIdentifier = findDirectoryOnDrive(
                driveIdentifier, driverRoot.getId(), pathIterator);
        if (directoryIdentifier == null) {
            LOG.warn("Can not find path '{}'", path);
            return Collections.emptyList();
        }
        return listDriveDirectoryByIdentifier(driveIdentifier, directoryIdentifier);
    }

    /**
     * Returns driveIdentifier for a drive with the given drive name.
     */
    protected String findDriveByName(
            String siteIdentifier, String driveName)
            throws SharepointException {
        var diskRequestBuilder = graphServiceClient
                .sites().bySiteId(siteIdentifier)
                .drives();
        var response = diskRequestBuilder.get();
        if (response == null) {
            throw new SharepointException("Can't list drives!");
        }
        var pages = response.getValue();
        while (pages != null) {
            // Process response
            for (Drive item : pages) {
                if (driveName.equals(item.getName())) {
                    return item.getId();
                }
            }
            // Check next page or end.
            if (response.getOdataNextLink() == null) {
                // There is no more pages, we can terminate.
                break;
            } else {
                // Move to the next page.
                response = diskRequestBuilder
                        .withUrl(response.getOdataNextLink())
                        .get();
                if (response == null) {
                    break;
                }
                pages = response.getValue();
            }
        }
        return null;
    }

    /**
     * Returns driveIdentifier for folder of given name in given folder.
     */
    protected String findDirectoryOnDrive(
            String driveIdentifier,
            String folderIdentifier,
            Iterator<String> pathIterator) throws SharepointException {
        if (!pathIterator.hasNext()) {
            return folderIdentifier;
        }
        var folderName = pathIterator.next();
        var driveBuilder = graphServiceClient
                .drives().byDriveId(driveIdentifier)
                .items().byDriveItemId(folderIdentifier)
                .children();
        var response = driveBuilder.get();
        if (response == null) {
            throw new SharepointException("Can't list folder content!");
        }
        var pages = response.getValue();
        while (pages != null) {
            // Process page data.
            for (var item : pages) {
                if (folderName.equals(item.getName())) {
                    // Recursion for the win ...
                    return findDirectoryOnDrive(driveIdentifier, item.getId(), pathIterator);
                }
            }
            // Check next page or end.
            if (response.getOdataNextLink() == null) {
                // There is no more pages, we can terminate.
                break;
            } else {
                // Move to the next page.
                response = driveBuilder
                        .withUrl(response.getOdataNextLink())
                        .get();
                if (response == null) {
                    break;
                }
                pages = response.getValue();
            }
        }
        // Ok we can not find what we are looking for.
        LOG.warn("Can't find '{}'.", folderName);
        return null;
    }

    protected List<SharepointFile> listDriveDirectoryByIdentifier(
            String driveIdentifier, String directoryIdentifier) throws SharepointException {

        var driveBuilder = graphServiceClient
                .drives().byDriveId(driveIdentifier)
                .items().byDriveItemId(directoryIdentifier)
                .children();
        var response = driveBuilder.get();
        if (response == null) {
            throw new SharepointException("Can't list folder content!");
        }
        var pages = response.getValue();
        var result = new ArrayList<SharepointFile>();
        while (pages != null) {
            // Process page data.
            for (var item : pages) {
                result.add(new SharepointFile(driveIdentifier, item.getId(), item.getName()));
            }
            // Check next page or end.
            if (response.getOdataNextLink() == null) {
                // There is no more pages, we can terminate.
                break;
            } else {
                // Move to the next page.
                response = driveBuilder
                        .withUrl(response.getOdataNextLink())
                        .get();
                if (response == null) {
                    break;
                }
                pages = response.getValue();
            }
        }
        return result;
    }

    public void downloadFile(SharepointFile file, Path outputDirectory)
            throws IOException, SharepointException {
        InputStream source = graphServiceClient
                .drives().byDriveId(file.driveIdentifier)
                .items().byDriveItemId(file.fileIdentifier)
                .content().get();
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
