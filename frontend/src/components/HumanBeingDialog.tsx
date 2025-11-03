import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  FormLabel,
  FormControlLabel,
  RadioGroup,
  Radio,
  Select,
  MenuItem,
  InputLabel,
  Grid,
  Alert,
  Box,
  Switch,
  Typography,
  Snackbar
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { HumanBeing, Mood, WeaponType, CreateHumanBeingRequest, Car } from '../types';
import { HumanBeingApi } from '../services/api';
import { humanBeingSchema } from '../validation/schema';
import ValidatedTextField from './ValidatedTextField';
import ValidationError from './ValidationError';

interface HumanBeingDialogProps {
  open: boolean;
  onClose: () => void;
  onSave: () => void;
  humanBeing?: HumanBeing | null;
}

const HumanBeingDialog: React.FC<HumanBeingDialogProps> = ({ 
  open, 
  onClose, 
  onSave, 
  humanBeing 
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [showSnackbar, setShowSnackbar] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [cars, setCars] = useState<Car[]>([]);
  const [useExistingCar, setUseExistingCar] = useState(false);
  const isEditMode = Boolean(humanBeing?.id);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isValid }
  } = useForm({
    resolver: yupResolver(humanBeingSchema) as any,
    mode: 'onChange',
    defaultValues: {
      name: '',
      coordinates: { x: 0, y: 0 },
      realHero: false,
      hasToothpick: null as boolean | null,
      car: { name: '', cool: false },
      mood: Mood.CALM,
      impactSpeed: 0,
      soundtrackName: '',
      minutesOfWaiting: 0,
      weaponType: WeaponType.AXE
    }
  });

  const realHero = watch('realHero');
  const weaponType = watch('weaponType');
  const impactSpeed = watch('impactSpeed');

  // Автоматическая установка impactSpeed=20 для MACHINE_GUN
  useEffect(() => {
    if (weaponType === WeaponType.MACHINE_GUN && (impactSpeed === 0 || impactSpeed === null || impactSpeed === undefined)) {
      setValue('impactSpeed', 20, { shouldValidate: true });
    }
  }, [weaponType, impactSpeed, setValue]);

  useEffect(() => {
    if (open) {
      const loadCars = async () => {
        try {
          const existingCars = await HumanBeingApi.getAllCars();
          setCars(existingCars);
        } catch (err) {
          console.error('Error loading cars:', err);
        }
      };
      loadCars();
    }
  }, [open]);

  useEffect(() => {
    if (humanBeing && open) {
      reset({
        name: humanBeing.name,
        coordinates: humanBeing.coordinates,
        realHero: humanBeing.realHero,
        hasToothpick: humanBeing.hasToothpick as boolean | null,
        car: humanBeing.car,
        mood: humanBeing.mood,
        impactSpeed: humanBeing.impactSpeed,
        soundtrackName: humanBeing.soundtrackName,
        minutesOfWaiting: humanBeing.minutesOfWaiting,
        weaponType: humanBeing.weaponType
      } as any);
    } else if (open && !humanBeing) {
      reset({
        name: '',
        coordinates: { x: 0, y: 0 },
        realHero: false,
        hasToothpick: null,
        car: { name: '', cool: false },
        mood: Mood.CALM,
        impactSpeed: 0,
        soundtrackName: '',
        minutesOfWaiting: 0,
        weaponType: WeaponType.AXE
      });
    }
  }, [humanBeing, open, reset]);

  const onSubmit = async (data: CreateHumanBeingRequest) => {
    setLoading(true);
    setError(null);
    setValidationErrors([]);

    try {
      if (isEditMode && humanBeing?.id) {
        await HumanBeingApi.update(humanBeing.id, data);
        setSnackbarMessage('Human Being updated successfully!');
      } else {
        await HumanBeingApi.create(data);
        setSnackbarMessage('Human Being created successfully!');
      }
      
      setShowSnackbar(true);
      onSave();
      onClose();
    } catch (err: any) {
      console.error('Error saving Human Being:', err);

      if (err.response?.data?.type === 'VALIDATION_ERROR') {
        if (err.response.data.fieldErrors) {
          const fieldErrors = Object.values(err.response.data.fieldErrors).flat() as string[];
          setValidationErrors(fieldErrors);
        } else {
          setValidationErrors([err.response.data.message || 'Validation failed']);
        }
      } else {
        setError(err.message || 'Failed to save Human Being');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      setError(null);
      setValidationErrors([]);
      onClose();
    }
  };

  return (
    <>
      <Dialog 
        open={open} 
        onClose={handleClose} 
        maxWidth="md" 
        fullWidth
        disableEscapeKeyDown={loading}
      >
        <DialogTitle>
          {isEditMode ? 'Edit Human Being' : 'Create New Human Being'}
        </DialogTitle>
        
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          
          {validationErrors.length > 0 && (
            <ValidationError 
              errors={validationErrors} 
              type="error" 
              title="Validation Errors"
            />
          )}

          <Box component="form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Controller
                  name="name"
                  control={control}
                  render={({ field }) => (
                    <ValidatedTextField
                      {...field}
                      label="Name *"
                      fullWidth
                      validationType="name"
                      maxLength={100}
                      error={!!errors.name}
                      helperText={errors.name?.message}
                      disabled={loading}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <Controller
                  name="coordinates.x"
                  control={control}
                  render={({ field }) => (
                    <ValidatedTextField
                      {...field}
                      label="X Coordinate *"
                      type="number"
                      fullWidth
                      validationType="coordinateX"
                      error={!!errors.coordinates?.x}
                      helperText={errors.coordinates?.x?.message}
                      disabled={loading}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <Controller
                  name="coordinates.y"
                  control={control}
                  render={({ field }) => (
                    <ValidatedTextField
                      {...field}
                      label="Y Coordinate *"
                      type="number"
                      fullWidth
                      validationType="coordinateY"
                      error={!!errors.coordinates?.y}
                      helperText={errors.coordinates?.y?.message}
                      disabled={loading}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Controller
                      name="realHero"
                      control={control}
                      render={({ field }) => (
                        <Switch
                          {...field}
                          checked={field.value}
                          disabled={loading}
                        />
                      )}
                    />
                  }
                  label="Real Hero"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <FormLabel>Has Toothpick</FormLabel>
                  <Controller
                    name="hasToothpick"
                    control={control}
                    render={({ field }) => (
                      <RadioGroup
                        {...field}
                        row
                      >
                        <FormControlLabel value={true} control={<Radio />} label="Yes" />
                        <FormControlLabel value={false} control={<Radio />} label="No" />
                        <FormControlLabel value={null} control={<Radio />} label="Unknown" />
                      </RadioGroup>
                    )}
                  />
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={useExistingCar}
                      onChange={(e) => setUseExistingCar(e.target.checked)}
                      disabled={loading}
                    />
                  }
                  label="Use Existing Car"
                />
              </Grid>

              {useExistingCar ? (
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel>Select Car *</InputLabel>
                    <Controller
                      name="car"
                      control={control}
                      render={({ field }) => (
                        <Select
                          {...field}
                          label="Select Car *"
                          disabled={loading}
                          value={(field.value as any)?.id || ''}
                          onChange={(e) => {
                            const selectedCar = cars.find(car => car.id === e.target.value);
                            if (selectedCar) {
                              field.onChange(selectedCar);
                            }
                          }}
                        >
                          {cars.map((car) => (
                            <MenuItem key={car.id} value={car.id}>
                              {car.name || 'Unnamed Car'} ({car.cool ? 'Cool' : 'Not Cool'})
                            </MenuItem>
                          ))}
                        </Select>
                      )}
                    />
                  </FormControl>
                </Grid>
              ) : (
                <>
                  <Grid item xs={12} sm={6}>
                    <Controller
                      name="car.name"
                      control={control}
                      render={({ field }) => (
                        <ValidatedTextField
                          {...field}
                          label="Car Name (Optional)"
                          fullWidth
                          validationType="name"
                          maxLength={50}
                          disabled={loading}
                        />
                      )}
                    />
                  </Grid>

                  <Grid item xs={12} sm={6}>
                    <FormControlLabel
                      control={
                        <Controller
                          name="car.cool"
                          control={control}
                          render={({ field }) => (
                            <Switch
                              {...field}
                              checked={field.value}
                              disabled={loading}
                            />
                          )}
                        />
                      }
                      label="Car is Cool"
                    />
                  </Grid>
                </>
              )}

              <Grid item xs={12} sm={6}>
                <Controller
                  name="impactSpeed"
                  control={control}
                  render={({ field }) => {
                    // Проверка для MACHINE_GUN
                    const isMachineGun = weaponType === WeaponType.MACHINE_GUN;
                    const hasError = !!errors.impactSpeed || (isMachineGun && field.value < 20);
                    const helperText = hasError 
                      ? (isMachineGun && field.value < 20 
                          ? 'MACHINE_GUN requires impactSpeed >= 20' 
                          : errors.impactSpeed?.message)
                      : '';
                    
                    return (
                      <ValidatedTextField
                        {...field}
                        label="Impact Speed *"
                        type="number"
                        fullWidth
                        validationType="impactSpeed"
                        realHero={realHero}
                        error={hasError}
                        helperText={helperText}
                        disabled={loading}
                        sx={{
                          '& .MuiOutlinedInput-root': {
                            '&.Mui-focused fieldset': {
                              borderColor: hasError ? 'error.main' : undefined
                            },
                            ...(hasError && {
                              '& fieldset': {
                                borderColor: 'error.main'
                              }
                            })
                          }
                        }}
                      />
                    );
                  }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <Controller
                  name="minutesOfWaiting"
                  control={control}
                  render={({ field }) => (
                    <ValidatedTextField
                      {...field}
                      label="Minutes of Waiting *"
                      type="number"
                      fullWidth
                      validationType="minutesOfWaiting"
                      error={!!errors.minutesOfWaiting}
                      helperText={errors.minutesOfWaiting?.message}
                      disabled={loading}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControl fullWidth error={!!errors.mood}>
                  <InputLabel>Mood *</InputLabel>
                  <Controller
                    name="mood"
                    control={control}
                    render={({ field }) => (
                      <Select
                        {...field}
                        label="Mood *"
                        disabled={loading}
                      >
                        {Object.values(Mood).map((mood) => (
                          <MenuItem key={mood} value={mood}>
                            {mood}
                          </MenuItem>
                        ))}
                      </Select>
                    )}
                  />
                  {errors.mood && (
                    <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 2 }}>
                      {errors.mood.message}
                    </Typography>
                  )}
                </FormControl>
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControl fullWidth error={!!errors.weaponType}>
                  <InputLabel>Weapon Type *</InputLabel>
                  <Controller
                    name="weaponType"
                    control={control}
                    render={({ field }) => (
                      <Select
                        {...field}
                        label="Weapon Type *"
                        disabled={loading}
                      >
                        {Object.values(WeaponType).map((weapon) => (
                          <MenuItem key={weapon} value={weapon}>
                            {weapon}
                          </MenuItem>
                        ))}
                      </Select>
                    )}
                  />
                  {errors.weaponType && (
                    <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 2 }}>
                      {errors.weaponType.message}
                    </Typography>
                  )}
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <Controller
                  name="soundtrackName"
                  control={control}
                  render={({ field }) => (
                    <ValidatedTextField
                      {...field}
                      label="Soundtrack Name *"
                      fullWidth
                      validationType="name"
                      maxLength={100}
                      error={!!errors.soundtrackName}
                      helperText={errors.soundtrackName?.message}
                      disabled={loading}
                    />
                  )}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>

        <DialogActions>
          <Button 
            onClick={handleClose} 
            disabled={loading}
            color="secondary"
          >
            Cancel
          </Button>
          <Button 
            onClick={handleSubmit(onSubmit)} 
            variant="contained" 
            disabled={loading || !isValid}
            color="primary"
          >
            {loading ? 'Saving...' : (isEditMode ? 'Update' : 'Create')}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={showSnackbar}
        autoHideDuration={3000}
        onClose={() => setShowSnackbar(false)}
        message={snackbarMessage}
      />
    </>
  );
};

export default HumanBeingDialog;
