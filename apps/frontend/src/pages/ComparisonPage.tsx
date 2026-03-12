/**
 * ComparisonPage - Full page wrapper for Advanced Comparison
 */

import React from 'react';
import { Page } from '@fluentui/react-components';
import { AdvancedComparison } from '../components/AdvancedComparison';

export const ComparisonPage: React.FC = () => {
    return (
        <Page>
            <AdvancedComparison embedded={true} />
        </Page>
    );
};

export default ComparisonPage;