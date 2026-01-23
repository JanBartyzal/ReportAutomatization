import models
from dbmodels import UploadFile as DBUploadFile
from database import SessionLocal
import os
import re
from powerpoint import PowerpointManager
from models import SlideData
import json
from pptx import Presentation
from table_data import TableDataProcessor


class OpexManager:
    def __init__(self):
        self.powerpoint_manager = PowerpointManager()
        self.table_data_processor = TableDataProcessor()

    def process_opex(self, file_id: str):
        db = SessionLocal()
        db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
        if not db_file:
            raise HTTPException(status_code=404, detail="File not found")
        file_path = "local_data/uploads/" + db_file.filename
        print(file_path)
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="File not found")
        presentation = self.powerpoint_manager.load_powerpoint(file_path)
        slides = self.powerpoint_manager.extract_slides(presentation)

        for slide in slides:
            print(slide)
            

    def get_presetation_header(self, file_id: str):
        db = SessionLocal()
        db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
        if not db_file:
            raise HTTPException(status_code=404, detail="File not found")
        file_path = "local_data/uploads/" + db_file.filename
        print(file_path)
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="File not found")
        presentation = self.powerpoint_manager.load_powerpoint(file_path)
        slides = self.powerpoint_manager.extract_slides(presentation)
        headdata=[]

        for slide in slides:
            info={
                "slide_id":slide.slide_index,
                "slide_title":slide.title,
                "table_data":len(slide.table_data),
                "image_data":len(slide.image_data),
                "text_content":len(slide.text_content)
            }
            headdata.append(info)

        print(headdata)
        return headdata

    def get_slide_data(self, file_id: str, slide_id: int):
            db = SessionLocal()
            db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
            if not db_file:
                raise HTTPException(status_code=404, detail="File not found")
            file_path = "local_data/uploads/" + db_file.filename
            print(file_path)
            if not os.path.exists(file_path):
                raise HTTPException(status_code=404, detail="File not found")
            presentation = self.powerpoint_manager.load_powerpoint(file_path)
            slides = self.powerpoint_manager.extract_slides(presentation)
            headdata=[]

            for slide in slides:
                if slide.slide_index == slide_id:
                    print("Slide found")
                    print(slide)
                    print("Slide text content")
                    print(slide.text_content)
                    print("Slide table data")
                    print(slide.table_data)
                    textslide=self.table_data_processor.normalize_text(slide.text_content)
                    tableslide=self.table_data_processor.normalize_table(slide.table_data)
                    
                    tables_list = []
                    # normalize_table returns a DataFrame. Validate it's not empty before converting.
                    if tableslide is not None and not tableslide.empty:
                         tables_list.append(tableslide.to_dict(orient='records'))

                    info={
                        "slide_id":slide.slide_index,
                        "slide_title":slide.title,
                        "text_content":textslide,
                        "table_data": tables_list,
                        "image_data_count":len(slide.image_data),
                        "text_content_count":len(slide.text_content)
                    }
                    headdata.append(info)

            print(headdata)
            return headdata

        





        


