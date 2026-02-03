package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.TransferRequest;
import com.pos.inventsight.model.sql.TransferRequestStatus;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for calculating transfer request permissions based on user role, transfer status,
 * and location access. This centralizes permission logic to avoid duplication between
 * frontend and backend.
 */
@Service
public class TransferPermissionService {

    /**
     * Roles that have GM+ privileges (can approve transfers)
     */
    private static final List<UserRole> GM_PLUS_ROLES = Arrays.asList(
        UserRole.OWNER,
        UserRole.FOUNDER,
        UserRole.ADMIN,
        UserRole.MANAGER
    );

    /**
     * Calculate available actions for a transfer based on user permissions
     * 
     * @param transfer The transfer request
     * @param user The current user
     * @return List of action names the user can perform
     */
    public List<String> getAvailableActions(TransferRequest transfer, User user) {
        List<String> actions = new ArrayList<>();

        if (user == null || transfer == null) {
            return actions;
        }

        boolean isGMPlus = isGMPlusRole(user);
        boolean isRequester = isUserRequester(transfer, user);
        boolean hasFromLocationAccess = hasLocationAccess(user, transfer.getFromLocationId(), transfer.getFromLocationType());
        boolean hasToLocationAccess = hasLocationAccess(user, transfer.getToLocationId(), transfer.getToLocationType());

        TransferRequestStatus status = transfer.getStatus();

        // PENDING Status Actions
        if (status == TransferRequestStatus.PENDING) {
            // GM+ users at FROM location can approve/reject
            if (isGMPlus && hasFromLocationAccess) {
                actions.add("approve");
                actions.add("reject");
            }
            
            // Requester can cancel their own pending transfers
            if (isRequester) {
                actions.add("cancel");
            }
        }

        // APPROVED Status Actions
        if (status == TransferRequestStatus.APPROVED) {
            // GM+ users or employees at FROM location can mark as ready
            if (isGMPlus && hasFromLocationAccess) {
                actions.add("markReady");
            }
        }

        // READY Status Actions
        if (status == TransferRequestStatus.READY) {
            // Delivery personnel or GM+ can start delivery
            if (isGMPlus) {
                actions.add("startDelivery");
            }
        }

        // IN_TRANSIT Status Actions
        if (status == TransferRequestStatus.IN_TRANSIT) {
            // Delivery personnel can mark as delivered
            if (isGMPlus) {
                actions.add("markDelivered");
            }
        }

        // DELIVERED Status Actions
        if (status == TransferRequestStatus.DELIVERED) {
            // Users at TO location can confirm receipt
            if (hasToLocationAccess || isGMPlus) {
                actions.add("receive");
            }
        }

        // GM+ can cancel at any stage except COMPLETED/CANCELLED/REJECTED
        if (isGMPlus && 
            status != TransferRequestStatus.COMPLETED && 
            status != TransferRequestStatus.CANCELLED &&
            status != TransferRequestStatus.REJECTED) {
            if (!actions.contains("cancel")) {
                actions.add("cancel");
            }
        }

        return actions;
    }

    /**
     * Check if user has GM+ role
     */
    private boolean isGMPlusRole(User user) {
        if (user.getRole() == null) {
            return false;
        }
        return GM_PLUS_ROLES.contains(user.getRole());
    }

    /**
     * Check if user is the requester of the transfer
     */
    private boolean isUserRequester(TransferRequest transfer, User user) {
        if (transfer.getRequestedByUserId() == null) {
            return false;
        }
        return transfer.getRequestedByUserId().equals(user.getId());
    }

    /**
     * Check if user has access to a specific location
     * 
     * @param user Current user
     * @param locationId Location ID
     * @param locationType Location type (STORE or WAREHOUSE)
     * @return true if user has access
     */
    private boolean hasLocationAccess(User user, UUID locationId, String locationType) {
        if (user == null || locationId == null) {
            return false;
        }

        // GM+ users have access to all locations
        if (isGMPlusRole(user)) {
            return true;
        }

        // TODO: Implement proper location access check
        // This requires user-location assignment data
        // For now, GM+ users have access to all locations
        // Regular users would need location assignment records
        
        return false;
    }

    /**
     * Validate if user can perform a specific action on a transfer
     * Used by action endpoints to double-check permissions
     * 
     * @param transfer The transfer request
     * @param user The current user
     * @param action The action to validate (e.g., "approve", "reject")
     * @return true if action is allowed
     */
    public boolean canPerformAction(TransferRequest transfer, User user, String action) {
        List<String> availableActions = getAvailableActions(transfer, user);
        return availableActions.contains(action);
    }
}
