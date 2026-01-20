export interface OpexFile {
    id: string;
    name: string;
    status: 'pending' | 'processing' | 'completed' | 'failed' | 'queued';
    created_at: string;
}

export interface OpexData {
    id: string;
    name: string;


}