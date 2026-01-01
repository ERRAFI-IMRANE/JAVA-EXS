from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from ultralytics import YOLO
import cv2
import numpy as np
import os

app = FastAPI()

# Load YOLO model once
model = YOLO("yolov8n.pt")  # or your custom trained model

@app.post("/detect")
async def detect_image(file: UploadFile = File(...)):
    # Read image bytes
    img_bytes = await file.read()
    nparr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        return JSONResponse({"error": "Invalid image"}, status_code=400)

    # Run YOLO detection
    results = model(img)

    output_info = []
    for r in results:
        for box, cls_id, score in zip(r.boxes.xyxy, r.boxes.cls, r.boxes.conf):
            x1, y1, x2, y2 = map(int, box)
            class_name = model.names[int(cls_id)]
            cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(img, f"{class_name} {score:.2f}", (x1, y1 - 5),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
            output_info.append({"type": class_name, "confidence": float(score)})

    # Save output image temporarily
    output_path = "output.jpg"
    cv2.imwrite(output_path, img)

    return JSONResponse({
        "output_image": output_path,  # client can download from this path
        "objects": output_info,
        "count": len(output_info)
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)


