package com.shipmanagement.util;

import com.shipmanagement.model.User;

import spark.HaltException;
import spark.Request;
import spark.Response;
import static spark.Spark.halt;

/**
 * Utility class for handling authentication-related operations.
 */
public class AuthUtil {
    
    /**
     * Checks if a user is authenticated.
     * @param req The HTTP request
     * @return true if the user is authenticated, false otherwise
     */
    public static boolean isAuthenticated(Request req) {
        return req.session().attribute("user") != null;
    }
    
    /**
     * Checks if the current user has admin privileges.
     * @param req The HTTP request
     * @return true if the user is an admin, false otherwise
     */
    public static boolean isAdmin(Request req) {
        User user = req.session().attribute("user");
        return user != null && "admin".equals(user.getRole());
    }
    
    /**
     * Requires authentication for the current request.
     * If the user is not authenticated, halts the request with a 401 status.
     * 
     * @param req The HTTP request
     * @param res The HTTP response
     * @throws HaltException if the user is not authenticated
     */
    public static void requireAuth(Request req, Response res) {
        if (!isAuthenticated(req)) {
            halt(401, "{\"error\":\"Authentication required\"}");
        }
    }
    
    /**
     * Requires admin privileges for the current request.
     * If the user is not an admin, halts the request with a 403 status.
     * 
     * @param req The HTTP request
     * @param res The HTTP response
     * @throws HaltException if the user is not an admin
     */
    public static void requireAdmin(Request req, Response res) {
        requireAuth(req, res);
        if (!isAdmin(req)) {
            halt(403, "{\"error\":\"Admin privileges required\"}");
        }
    }
}
