from spire.presentation.common import *
from spire.presentation import *
import os
from pathlib import Path

class PPTX2Image:
    def __init__(self, input_path: str, output_path: str):
        self.input_path = input_path
        self.output_path = output_path
        self.presentation = Presentation()

    def convert(self):
        self.presentation.LoadFromFile(self.input_path)
        # Load the PowerPoint file
        try:
            self.presentation.LoadFromFile(self.input_path)
            print("Presentation loaded successfully.")
            # Iterate through each slide in the presentation
            for i, slide in enumerate(self.presentation.Slides):
                # Save each slide as an image
                # The SaveAsImage() method returns a System.Drawing.Image object
                image = slide.SaveAsImage()
                # Define the output file name for the image
                # We'll use a descriptive name like "slide_0.png", "slide_1.png", etc.
                output_image_path = f"{self.output_path}/slide_{i}.png"
                # Save the image to the specified path and format
                # Use ImageFormat.Png for PNG, ImageFormat.Jpeg for JPEG, etc.
                image.Save(output_image_path)
                print(f"Slide {i+1} saved as {output_image_path}")
                # Dispose of the image object to free up resources
                image.Dispose()

            print("All slides converted to images successfully.")
        except Exception as e:
            print(f"An error occurred during conversion: {e}")
        finally:
            # Always dispose of the presentation object to release resources
            self.presentation.Dispose()

    def convert1slide(self, slide_index: int):
        self.presentation.LoadFromFile(self.input_path)
        slide = self.presentation.Slides[slide_index]
        image = slide.SaveAsImage()
        output_image_path = f"{self.output_path}/slide_{slide_index}.png"
        
        image.Save(output_image_path)
        print(f"Slide {slide_index+1} saved as {output_image_path}")
        image.Dispose()
        self.presentation.Dispose()

def convert_ppt_to_images(input_path, pptx_file, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    pptx_file = os.path.join(input_path, pptx_file)
 
    # Validate pptx_file path
    if not os.path.isfile(pptx_file):
        print(f"Error: PPTX file '{pptx_file}' not found.")
        return

    # Specify the file name
    pdf_file = os.path.join(output_dir, Path(pptx_file).stem + ".pdf")
     
    # Creating a file at specified location
    with open(os.path.join(input_path, pdf_file), 'w') as fp:
        pass
        
    convert_command = f"soffice --headless --convert-to pdf \"{pptx_file}\" --outdir \"{output_dir}\""

    os.system(convert_command)

    # Check if PDF conversion was successful
    if not os.path.isfile(pdf_file):
        print(f"Error: PDF file '{pdf_file}' could not be created.")
        return
       
    # Convert each page of the PDF to an image
    pdftoppm_command = f"pdftoppm -png \"{pdf_file}\" \"{output_dir}/slide\""
    os.system(pdftoppm_command)

    # Rename output files to slide_1.png, slide_2.png, etc.
    for i, file_name in enumerate(sorted(os.listdir(output_dir))):
        if file_name.startswith('slide-') and file_name.endswith('.png'):
            os.rename(
                os.path.join(output_dir, file_name),
                os.path.join(output_dir, f"slide_{i + 1}.png")
            )
    print(f"Slides from {pptx_file} have been converted to images in {output_dir}")
