/**
 * ComparisonPage — migrated per P9-W2-005
 * Wrapped with PageHeader; removed <Page> non-standard wrapper.
 */
import { makeStyles, tokens } from '@fluentui/react-components';
import { PageHeader } from '../components/shared/PageHeader';
import { AdvancedComparison } from '../components/AdvancedComparison/AdvancedComparison';

const useStyles = makeStyles({
    container: {
        padding: tokens.spacingHorizontalL,
    },
    content: {
        marginTop: tokens.spacingVerticalM,
    },
});

export default function ComparisonPage() {
    const styles = useStyles();

    return (
        <div className={styles.container}>
            <PageHeader
                title="Comparison"
                subtitle="Compare metrics and data across periods, organizations, and time ranges"
            />
            <div className={styles.content}>
                <AdvancedComparison />
            </div>
        </div>
    );
}