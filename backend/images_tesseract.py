import pytesseract
from pytesseract import Output
from img2table.document import Image as TableImage
from img2table.ocr import TesseractOCR
import numpy as np
import cv2
import io
from PIL import Image


class TableImageData:
    def __init__(self):
        self.ocr_engine = TesseractOCR(n_threads=1, lang="eng") 

    def get_tesseract_confidence(self, pil_image):
        """
        Vrátí průměrnou jistotu (0-100) a odhadnutý název (první řádek).
        """
        # Získáme detailní data o každém slovu
        data = pytesseract.image_to_data(pil_image, output_type=Output.DICT, lang='ces')
        
        confidences = []
        lines = {}
        
        n_boxes = len(data['text'])
        for i in range(n_boxes):
            # Ignorujeme prázdná místa a velmi nízkou confidence (-1)
            text = data['text'][i].strip()
            conf = int(data['conf'][i])
            
            if conf > 0 and len(text) > 0:
                confidences.append(conf)
                
                # Skládání řádků pro detekci nadpisu (podle souřadnice top)
                top = data['top'][i]
                # Zjednodušení: shlukujeme texty, které jsou ve stejném "řádku" (tolerance 10px)
                found_line = False
                for y in lines.keys():
                    if abs(y - top) < 10:
                        lines[y].append(text)
                        found_line = True
                        break
                if not found_line:
                    lines[top] = [text]

        # Výpočet průměrné jistoty
        avg_conf = sum(confidences) / len(confidences) if confidences else 0
        
        # Odhad názvu: Vezmeme první řádek textu
        sorted_ys = sorted(lines.keys())
        title = " ".join(lines[sorted_ys[0]]) if sorted_ys else "Unknown Table"

        return avg_conf, title

    def smart_extract(self, image_bytes):
        """
        Hlavní rozhodovací funkce.
        """
        image_io = io.BytesIO(image_bytes)
        pil_img = Image.open(image_io)

        # 1. Rychlá analýza Tesseractem (Confidence + Title)
        confidence, detected_title = self.get_tesseract_confidence(pil_img)
        print(f"OCR Confidence: {confidence}%, Title estimate: {detected_title}")

        result = {
            "title": detected_title,
            "method": "AI_VISION", # Defaultně chceme AI, pokud to nebude perfektní
            "data": None,
            "confidence": confidence
        }

        # 2. Pokus o extrakci tabulky pomocí img2table (lokálně)
        # Reset streamu
        image_io.seek(0)
        doc = TableImage(image_io)
        
        try:
            # extract_tables vrací list tabulek
            extracted_tables = doc.extract_tables(ocr=self.ocr_engine, borderless_tables=True)
            
            if extracted_tables and len(extracted_tables) > 0:
                # Našli jsme tabulku!
                table = extracted_tables[0]
                df = table.df
                
                # 3. Logika rozhodování (Semafory)
                # Pokud je confidence vysoká A tabulka má rozumný tvar (ne 1 sloupec, ne 0 řádků)
                if confidence > 85 and df.shape[1] > 1 and df.shape[0] > 1:
                    print(" -> používám lokální OCR data.")
                    result["method"] = "LOCAL_OCR"
                    result["data"] = df.to_dict(orient='records')
                else:
                    print(" -> Nalezena tabulka, ale OCR si není jisté. Jdu na AI.")
            else:
                print(" -> img2table nenašel mřížku tabulky. Jdu na AI.")

        except Exception as e:
            print(f"Chyba při img2table: {e}")

        return result