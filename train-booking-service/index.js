import express from 'express';
import cors from 'cors';
import { configure, searchTrainBetweenStations, getAvailability } from 'railkit';

const app = express();
const port = 3001;

app.use(cors());
app.use(express.json());

// Initialize railkit with the API key
const apiKey = process.env.RAILKIT_API_KEY || 'irctc_6a066efa933883a20ffdcd53ee29986c3f3e8a5676801210';
configure(apiKey);

app.get('/search', async (req, res) => {
    try {
        const { from, to, date } = req.query;
        if (!from || !to || !date) {
            return res.status(400).json({ error: 'Missing required parameters: from, to, date' });
        }

        console.log(`Searching trains from ${from} to ${to} on ${date}`);
        const result = await searchTrainBetweenStations(from, to, date);
        res.json(result);
    } catch (error) {
        console.error('Error fetching trains:', error);
        res.status(500).json({ error: 'Failed to fetch train data' });
    }
});

app.get('/availability', async (req, res) => {
    try {
        const { trainNo, from, to, date, class: travelClass, quota } = req.query;
        if (!trainNo || !from || !to || !date || !travelClass || !quota) {
            return res.status(400).json({ error: 'Missing required parameters' });
        }

        console.log(`Checking availability for ${trainNo} from ${from} to ${to} on ${date} class ${travelClass} quota ${quota}`);
        const result = await getAvailability(trainNo, from, to, date, travelClass, quota);
        res.json(result);
    } catch (error) {
        console.error('Error fetching availability:', error);
        res.status(500).json({ error: 'Failed to fetch train availability' });
    }
});

app.listen(port, () => {
    console.log(`Train Booking Proxy running on http://localhost:${port}`);
});
