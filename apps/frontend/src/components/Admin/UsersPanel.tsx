import { useUsers, useAssignRole } from '../../hooks/useAdmin';
import { Body1, Button, Spinner } from '@fluentui/react-components';

const UsersPanel: React.FC = () => {
    const { data, isLoading, error } = useUsers();

    if (isLoading) {
        return <Spinner label="Loading users..." />;
    }

    if (error) {
        return <Body1>Error loading users: {error.message}</Body1>;
    }

    return (
        <div>
            <Body1>User Management</Body1>
            <p>User list with role assignment functionality - implementation in progress</p>
        </div>
    );
};

export default UsersPanel;
