import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Title2, Card, CardHeader, Text,
    LargeTitle, Body1, Button, Spinner,
    Select, Accordion, AccordionHeader, AccordionItem, AccordionPanel,
    Image, Divider
} from '@fluentui/react-components';
import { ArrowLeft24Regular, ArrowRight24Regular } from '@fluentui/react-icons';
import { api } from '../../api/endpoints';

export const ViewerPPTX: React.FC = () => {
    const { fileId } = useParams<{ fileId: string }>();
    const navigate = useNavigate();
    const [files, setFiles] = useState<any[]>([]);
    const [selectedFileId, setSelectedFileId] = useState<string>(fileId || "");

    // Slide Data
    const [slidesHeader, setSlidesHeader] = useState<any[]>([]);
    const [currentSlideIndex, setCurrentSlideIndex] = useState<number>(0); // Index in slidesHeader array
    const [slideData, setSlideData] = useState<any>(null);
    const [loadingData, setLoadingData] = useState(false);

    useEffect(() => {
        loadFiles();
    }, []);

    useEffect(() => {
        if (fileId) {
            setSelectedFileId(fileId);
            handleFileSelectLogic(fileId);
        }
    }, [fileId]);

    const loadFiles = async () => {
        try {
            const data = await api.opex.listUploadedFiles();
            setFiles(data);
        } catch (e) {
            console.error(e);
        }
    };

    const handleFileSelect = async (e: React.ChangeEvent<HTMLSelectElement>) => {
        const newFileId = e.target.value;
        // Update URL to reflect selection
        navigate(`/opex/view/${newFileId}`);
        // Logic handled by useEffect on fileId param change, but strictly speaking we might want to just set it here if no param based routing was used
        // But since we want permalinks, navigating is better.
    };

    const handleFileSelectLogic = async (id: string) => {
        setSlidesHeader([]);
        setSlideData(null);
        setCurrentSlideIndex(0);

        if (id) {
            setLoadingData(true);
            try {
                const headers = await api.opex.getFileHeader(id);
                setSlidesHeader(headers);
                // Automatically fetch first slide if available
                if (headers && headers.length > 0) {
                    await loadSlide(id, headers[0].slide_id);
                }
            } catch (error) {
                console.error("Error loading headers", error);
            } finally {
                setLoadingData(false);
            }
        }
    };

    const loadSlide = async (fileId: string, slideId: number) => {
        setLoadingData(true);
        try {
            const data = await api.opex.getSlideData(fileId, slideId);
            if (data && data.length > 0) {
                setSlideData(data[0]);
            } else {
                setSlideData(null);
            }
        } catch (error) {
            console.error("Error loading slide", error);
        } finally {
            setLoadingData(false);
        }
    };

    const handlePrev = () => {
        if (currentSlideIndex > 0) {
            const newIndex = currentSlideIndex - 1;
            setCurrentSlideIndex(newIndex);
            loadSlide(selectedFileId, slidesHeader[newIndex].slide_id);
        }
    };

    const handleNext = () => {
        if (currentSlideIndex < slidesHeader.length - 1) {
            const newIndex = currentSlideIndex + 1;
            setCurrentSlideIndex(newIndex);
            loadSlide(selectedFileId, slidesHeader[newIndex].slide_id);
        }
    };

    return (
        <div style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title2>Opex Viewer</Title2>
            </div>

            {/* Browser */}
            <Title2>Prohlížeč dat</Title2>
            <Card>
                <div style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                        <Text>Vyberte soubor:</Text>
                        <Select onChange={handleFileSelect} value={selectedFileId}>
                            <option value="">-- Vyberte soubor --</option>
                            {files.map(f => (
                                <option key={f.id} value={f.id}>{f.filename}</option>
                            ))}
                        </Select>
                        {loadingData && <Spinner size="tiny" />}
                    </div>

                    {slidesHeader.length > 0 && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#f5f5f5', padding: '0.5rem', borderRadius: '4px' }}>
                            <Button icon={<ArrowLeft24Regular />} onClick={handlePrev} disabled={currentSlideIndex === 0}>Předchozí</Button>
                            <Text weight="semibold">Slide {currentSlideIndex + 1} / {slidesHeader.length}</Text>
                            <Button icon={<ArrowRight24Regular />} onClick={handleNext} disabled={currentSlideIndex === slidesHeader.length - 1}>Další</Button>
                        </div>
                    )}

                    {slideData && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', marginTop: '1rem' }}>
                            <div style={{ borderBottom: '1px solid #ccc', paddingBottom: '0.5rem' }}>
                                <Title2>{slideData.slide_title || "Untitled Slide"}</Title2>
                                <Text size={200} style={{ color: '#666' }}>Slide Index: {slideData.slide_id}</Text>
                            </div>

                            {/* Text Content */}
                            <div>
                                <Text weight="semibold" size={400}>Obsah textu:</Text>
                                <div style={{ marginTop: '0.5rem', whiteSpace: 'pre-wrap', background: '#fafafa', padding: '1rem', borderRadius: '4px', border: '1px solid #eee' }}>
                                    {slideData.text_content || "Žádný text"}
                                </div>
                            </div>

                            {/* Tables */}
                            <div>
                                <Text weight="semibold" size={400}>Tabulky ({Array.isArray(slideData.table_data) ? slideData.table_data.length : 0}):</Text>
                                {Array.isArray(slideData.table_data) && slideData.table_data.map((table: any, idx: number) => (
                                    <div key={idx} style={{ marginTop: '1rem', overflowX: 'auto' }}>
                                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
                                            <thead>
                                                <tr style={{ background: '#f0f0f0' }}>
                                                    {/* Assuming table is list of dicts, take keys from first row if exists */}
                                                    {table.length > 0 && Object.keys(table[0]).map(key => (
                                                        <th key={key} style={{ padding: '8px', border: '1px solid #ddd', textAlign: 'left' }}>{key}</th>
                                                    ))}
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {table.map((row: any, rIdx: number) => (
                                                    <tr key={rIdx}>
                                                        {Object.values(row).map((val: any, cIdx: number) => (
                                                            <td key={cIdx} style={{ padding: '8px', border: '1px solid #ddd' }}>{String(val)}</td>
                                                        ))}
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ))}
                                {(!slideData.table_data || slideData.table_data.length === 0) && (
                                    <Text style={{ display: 'block', marginTop: '0.5rem', fontStyle: 'italic', color: '#888' }}>Žádné tabulky</Text>
                                )}
                            </div>

                            <Divider />

                            {/* Debug Information */}
                            <Accordion collapsible>
                                <AccordionItem value="debug">
                                    <AccordionHeader>Debug Information (Extraction Details)</AccordionHeader>
                                    <AccordionPanel>
                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', padding: '1rem' }}>

                                            {/* Extracted Images */}
                                            <div>
                                                <Text weight="semibold" size={400}>Extrahované obrázky ({slideData.image_data?.length || 0}):</Text>
                                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginTop: '1rem' }}>
                                                    {slideData.image_data?.map((img: any, idx: number) => (
                                                        <div key={idx} style={{ border: '1px solid #ddd', padding: '5px', borderRadius: '4px' }}>
                                                            <img
                                                                src={`data:image/png;base64,${img.image_base64}`}
                                                                alt={`Extracted ${idx}`}
                                                                style={{ maxWidth: '200px', maxHeight: '200px', display: 'block' }}
                                                            />
                                                            <Text size={100} style={{ display: 'block', textAlign: 'center', marginTop: '4px' }}>Image {idx + 1}</Text>
                                                        </div>
                                                    ))}
                                                    {(!slideData.image_data || slideData.image_data.length === 0) && (
                                                        <Text size={200} italic>Žádné přímo extrahované obrázky</Text>
                                                    )}
                                                </div>
                                            </div>

                                            {/* Ollama Processing Image */}
                                            {slideData.ollama_slide_image && (
                                                <div>
                                                    <Text weight="semibold" size={400}>Stránka jako PNG (pro Ollama OCR validaci):</Text>
                                                    <div style={{ marginTop: '1rem', border: '1px solid #ccc', borderRadius: '8px', overflow: 'hidden', maxWidth: '800px' }}>
                                                        <img
                                                            src={`data:image/png;base64,${slideData.ollama_slide_image}`}
                                                            alt="Ollama Slide"
                                                            style={{ width: '100%', height: 'auto', display: 'block' }}
                                                        />
                                                    </div>
                                                </div>
                                            )}

                                            {/* Ollama Prompt */}
                                            {slideData.ollama_prompt && (
                                                <div>
                                                    <Text weight="semibold" size={400}>Ollama OCR Prompt:</Text>
                                                    <div style={{
                                                        marginTop: '0.5rem',
                                                        whiteSpace: 'pre-wrap',
                                                        background: '#1e1e1e',
                                                        color: '#d4d4d4',
                                                        padding: '1rem',
                                                        borderRadius: '4px',
                                                        fontFamily: 'Consolas, Monaco, "Andale Mono", "Ubuntu Mono", monospace',
                                                        fontSize: '0.85rem',
                                                        maxHeight: '400px',
                                                        overflowY: 'auto'
                                                    }}>
                                                        {slideData.ollama_prompt}
                                                    </div>
                                                </div>
                                            )}

                                            {!slideData.ollama_slide_image && !slideData.ollama_prompt && (
                                                <Text size={200} italic>Žádné Ollama OCR debug informace (pravděpodobně použita nativní extrakce)</Text>
                                            )}
                                        </div>
                                    </AccordionPanel>
                                </AccordionItem>
                            </Accordion>
                        </div>
                    )}
                </div>
            </Card>
        </div>
    );
};