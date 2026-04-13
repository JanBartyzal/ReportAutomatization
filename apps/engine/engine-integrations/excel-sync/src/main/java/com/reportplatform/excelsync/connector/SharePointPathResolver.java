package com.reportplatform.excelsync.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SharePointPathResolver {

    private static final Logger log = LoggerFactory.getLogger(SharePointPathResolver.class);

    // https://{tenant}.sharepoint.com/sites/{site}/Shared Documents/{path}
    private static final Pattern SHAREPOINT_DOCS_URL = Pattern.compile(
            "https://([^.]+)\\.sharepoint\\.com/sites/([^/]+)/([^?]+)");

    // https://{tenant}.sharepoint.com/:x:/s/{site}/{encoded-item-id}
    private static final Pattern SHAREPOINT_ENCODED_URL = Pattern.compile(
            "https://([^.]+)\\.sharepoint\\.com/:x:/s/([^/]+)/([^?]+)");

    public record ResolvedPath(String tenant, String siteName, String relativePath) {}

    public static ResolvedPath resolve(String sharepointUrl) {
        Matcher docsMatcher = SHAREPOINT_DOCS_URL.matcher(sharepointUrl);
        if (docsMatcher.matches()) {
            String tenant = docsMatcher.group(1);
            String site = docsMatcher.group(2);
            String path = docsMatcher.group(3);
            // Strip "Shared Documents/" prefix if present
            if (path.startsWith("Shared Documents/") || path.startsWith("Shared%20Documents/")) {
                path = path.substring(path.indexOf('/') + 1);
            }
            log.debug("Resolved SharePoint docs URL: tenant={}, site={}, path={}", tenant, site, path);
            return new ResolvedPath(tenant, site, path);
        }

        Matcher encodedMatcher = SHAREPOINT_ENCODED_URL.matcher(sharepointUrl);
        if (encodedMatcher.matches()) {
            String tenant = encodedMatcher.group(1);
            String site = encodedMatcher.group(2);
            String itemId = encodedMatcher.group(3);
            log.debug("Resolved SharePoint encoded URL: tenant={}, site={}, itemId={}", tenant, site, itemId);
            return new ResolvedPath(tenant, site, itemId);
        }

        throw new IllegalArgumentException("Cannot parse SharePoint URL: " + sharepointUrl);
    }

    public static String buildGraphSiteUrl(String tenant, String siteName) {
        return String.format("https://graph.microsoft.com/v1.0/sites/%s.sharepoint.com:/sites/%s",
                tenant, siteName);
    }

    public static String buildDriveItemByPathUrl(String siteId, String driveId, String filePath) {
        return String.format("https://graph.microsoft.com/v1.0/drives/%s/root:/%s:/content",
                driveId, filePath);
    }

    public static String buildDriveItemContentUrl(String driveId, String itemId) {
        return String.format("https://graph.microsoft.com/v1.0/drives/%s/items/%s/content",
                driveId, itemId);
    }

    public static String buildDrivesUrl(String siteId) {
        return String.format("https://graph.microsoft.com/v1.0/sites/%s/drives", siteId);
    }
}
