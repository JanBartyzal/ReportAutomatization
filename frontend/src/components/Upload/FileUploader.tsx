
import React, { useCallback, useState } from 'react';
import { useDropzone, FileRejection } from 'react-dropzone';
import { UploadCloud, File as FileIcon, X, CheckCircle, AlertCircle, FileSpreadsheet } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useUploadFile } from '../../api/files';

export const FileUploader: React.FC = () => {
    const [uploadProgress, setUploadProgress] = useState<number>(0);
    const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);

    const uploadFileMutation = useUploadFile();

    const onDrop = useCallback((acceptedFiles: File[]) => {
        if (acceptedFiles.length > 0) {
            setSelectedFile(acceptedFiles[0]);
            setUploadStatus('idle');
            setErrorMessage(null);
            setUploadProgress(0);
        }
    }, []);

    const onDropRejected = useCallback((fileRejections: FileRejection[]) => {
        const error = fileRejections[0]?.errors[0];
        setErrorMessage(error?.message || "Invalid file");
        setUploadStatus('error');
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        onDropRejected,
        maxFiles: 1,
        accept: {
            'application/vnd.openxmlformats-officedocument.presentationml.presentation': ['.pptx'],
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
        },
        maxSize: 50 * 1024 * 1024 // 50MB
    });

    const handleUpload = () => {
        if (!selectedFile) return;

        setUploadStatus('uploading');
        const isOpex = selectedFile.name.endsWith('.xlsx');

        uploadFileMutation.mutate(
            {
                file: selectedFile,
                isOpex,
                onProgress: (p) => setUploadProgress(p)
            },
            {
                onSuccess: () => {
                    setUploadStatus('success');
                    setUploadProgress(100);
                    // Clear file after 3 seconds
                    setTimeout(() => {
                        setSelectedFile(null);
                        setUploadStatus('idle');
                        setUploadProgress(0);
                    }, 3000);
                },
                onError: (error) => {
                    setUploadStatus('error');
                    setErrorMessage("Upload failed. Please try again.");
                    console.error(error);
                }
            }
        );
    };

    const removeFile = (e: React.MouseEvent) => {
        e.stopPropagation();
        setSelectedFile(null);
        setUploadStatus('idle');
    };

    return (
        <div className="w-full">
            <div
                {...getRootProps()}
                className={cn(
                    "relative border-2 border-dashed rounded-xl p-8 transition-all duration-300 text-center cursor-pointer overflow-hidden",
                    isDragActive
                        ? "border-blue-500 bg-blue-50"
                        : "border-slate-200 hover:border-slate-300 hover:bg-slate-50",
                    uploadStatus === 'error' && "border-red-300 bg-red-50",
                    uploadStatus === 'success' && "border-green-300 bg-green-50"
                )}
            >
                <input {...getInputProps()} />

                {/* Background Progress Bar */}
                {uploadStatus === 'uploading' && (
                    <div
                        className="absolute bottom-0 left-0 h-1 bg-blue-500 transition-all duration-300"
                        style={{ width: `${uploadProgress}%` }}
                    />
                )}

                <div className="flex flex-col items-center justify-center space-y-4 relative z-10">
                    {uploadStatus === 'success' ? (
                        <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center animate-in zoom-in">
                            <CheckCircle className="w-8 h-8" />
                        </div>
                    ) : uploadStatus === 'error' ? (
                        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center animate-in zoom-in">
                            <AlertCircle className="w-8 h-8" />
                        </div>
                    ) : selectedFile ? (
                        <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center animate-in zoom-in">
                            {selectedFile.name.endsWith('.xlsx') ? (
                                <FileSpreadsheet className="w-8 h-8" />
                            ) : (
                                <FileIcon className="w-8 h-8" />
                            )}
                        </div>
                    ) : (
                        <div className="w-16 h-16 bg-slate-100 text-slate-400 rounded-full flex items-center justify-center group-hover:scale-110 transition-transform">
                            <UploadCloud className="w-8 h-8" />
                        </div>
                    )}

                    <div className="space-y-1">
                        {selectedFile ? (
                            <>
                                <h3 className="text-lg font-semibold text-slate-900 line-clamp-1 break-all px-4">
                                    {selectedFile.name}
                                </h3>
                                <p className="text-sm text-slate-500">
                                    {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                                </p>
                            </>
                        ) : (
                            <>
                                <h3 className="text-lg font-semibold text-slate-900">
                                    Click to upload or drag and drop
                                </h3>
                                <p className="text-sm text-slate-500">
                                    Supports .pptx and .xlsx (max 50MB)
                                </p>
                            </>
                        )}
                    </div>

                    {errorMessage && (
                        <p className="text-sm text-red-600 font-medium animate-in fade-in slide-in-from-bottom-2">
                            {errorMessage}
                        </p>
                    )}

                    {selectedFile && uploadStatus === 'idle' && (
                        <div className="pt-4 flex items-center space-x-3 w-full max-w-xs mx-auto animate-in fade-in slide-in-from-bottom-2">
                            <button
                                onClick={(e) => { e.stopPropagation(); handleUpload(); }}
                                className="flex-1 py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg shadow-lg shadow-blue-900/20 font-medium transition-colors"
                            >
                                Upload File
                            </button>
                            <button
                                onClick={removeFile}
                                className="p-2 bg-slate-200 hover:bg-slate-300 text-slate-600 rounded-lg transition-colors"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};
